package myPaint.userTools;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/*
Lets the user select part of the canvas, then move or resize it.
- FREE: freehand lasso that follows the mouse path and closes when released.
- RECTANGLE: free-size rectangle dragged from the start point.

Once a selection exists, dragging inside it moves it and dragging one of its 8 handles resizes it.
The selected pixels are only lifted off the canvas on the first move/resize (the hole is filled white)
and are drawn on the overlay until the selection is committed. Committing happens when the user starts a
new selection or switches tools.

Written by: AJ Utz
Written on: 8/13/2026
Last Update: 10/29/2026
*/
public class SelectionTool {

	public enum SelectionVariant {
		FREE,
		RECTANGLE
	}

	private enum Mode {
		NONE,
		CREATING,
		MOVING,
		RESIZING
	}

	private static final double LINE_WIDTH = 1.0;
	private static final double HANDLE_SIZE = 8.0;
	private static final double HANDLE_HIT_RADIUS = 7.0;

	// Handle order: top-left, top, top-right, right, bottom-right, bottom, bottom-left, left
	private static final boolean[] HANDLE_LEFT = { true, false, false, false, false, false, true, true };
	private static final boolean[] HANDLE_RIGHT = { false, false, true, true, true, false, false, false };
	private static final boolean[] HANDLE_TOP = { true, true, true, false, false, false, false, false };
	private static final boolean[] HANDLE_BOTTOM = { false, false, false, false, true, true, true, false };
	private static final Cursor[] HANDLE_CURSORS = {
		Cursor.NW_RESIZE, Cursor.N_RESIZE, Cursor.NE_RESIZE, Cursor.E_RESIZE,
		Cursor.SE_RESIZE, Cursor.S_RESIZE, Cursor.SW_RESIZE, Cursor.W_RESIZE
	};

	private final GraphicsContext context;
	private final GraphicsContext imageContext;
	private final Canvas imageCanvas;
	private final double canvasWidth;
	private final double canvasHeight;
	private SelectionVariant currentVariant = SelectionVariant.FREE;
	private SelectionVariant activeSelectionVariant = SelectionVariant.FREE;
	private Mode mode = Mode.NONE;

	// Selection being drawn
	private double startX;
	private double startY;
	private double currentX;
	private double currentY;
	private final List<double[]> lassoPoints = new ArrayList<>();
	private boolean selecting;

	// Finished selection: where it was taken from (orig) and where it is now (box)
	private boolean hasSelection;
	private double origX;
	private double origY;
	private double origW;
	private double origH;
	private double boxX;
	private double boxY;
	private double boxW;
	private double boxH;
	private Image floating;

	// Move / resize drag state
	private double moveOffsetX;
	private double moveOffsetY;
	private int activeHandle = -1;
	private double edgeLeft;
	private double edgeRight;
	private double edgeTop;
	private double edgeBottom;

	public SelectionTool(GraphicsContext context, Canvas imageCanvas, double canvasWidth, double canvasHeight) {
		if (context == null || imageCanvas == null) {
			throw new IllegalArgumentException("Selection contexts cannot be null.");
		}
		if (canvasWidth <= 0 || canvasHeight <= 0) {
			throw new IllegalArgumentException("Canvas dimensions must be positive.");
		}
		this.context = context;
		this.imageCanvas = imageCanvas;
		this.imageContext = imageCanvas.getGraphicsContext2D();
		this.canvasWidth = canvasWidth;
		this.canvasHeight = canvasHeight;
		applySelectionStyle();
	}

	public void setSelectionVariant(SelectionVariant variant) {
		if (variant == null) {
			return;
		}
		// An existing selection keeps the variant it was made with
		this.currentVariant = variant;
	}

	public boolean hasSelection() {
		return hasSelection;
	}

	// Bounding box of the selection (for the lasso, the box around its path)
	public double getSelectionX() {
		return hasSelection ? boxX : creationBounds()[0];
	}

	public double getSelectionY() {
		return hasSelection ? boxY : creationBounds()[1];
	}

	public double getSelectionWidth() {
		return hasSelection ? boxW : creationBounds()[2];
	}

	public double getSelectionHeight() {
		return hasSelection ? boxH : creationBounds()[3];
	}

	// Mouse entry points. Coordinates are canvas-local.
	public void pressed(double x, double y) {
		if (hasSelection) {
			int handle = handleAt(x, y);
			if (handle >= 0) {
				beginResize(handle);
				return;
			}
			if (isInsideBox(x, y)) {
				mode = Mode.MOVING;
				moveOffsetX = x - boxX;
				moveOffsetY = y - boxY;
				return;
			}
			commit();
		}
		startSelection(x, y);
	}

	public void dragged(double x, double y) {
		switch (mode) {
			case CREATING:
				updateSelection(x, y);
				break;
			case MOVING:
				liftSelection();
				boxX = Math.clamp(x - moveOffsetX, 0, canvasWidth - boxW);
				boxY = Math.clamp(y - moveOffsetY, 0, canvasHeight - boxH);
				drawEditing();
				break;
			case RESIZING:
				liftSelection();
				resizeTo(clamp(x, canvasWidth), clamp(y, canvasHeight));
				drawEditing();
				break;
			default:
				break;
		}
	}

	public void released(double x, double y) {
		if (mode == Mode.CREATING) {
			endSelection(x, y);
		} else {
			mode = Mode.NONE;
			activeHandle = -1;
		}
	}

	// Stamps the moved/resized selection onto the canvas and removes the selection outline
	public void commit() {
		if (hasSelection && floating != null) {
			imageContext.drawImage(floating, boxX, boxY, boxW, boxH);
		}
		hasSelection = false;
		floating = null;
		mode = Mode.NONE;
		activeHandle = -1;
		selecting = false;
		lassoPoints.clear();
		clearOverlay();
	}

	public Cursor cursorAt(double x, double y) {
		if (!hasSelection || mode == Mode.CREATING) {
			return Cursor.CROSSHAIR;
		}
		int handle = handleAt(x, y);
		if (handle >= 0) {
			return HANDLE_CURSORS[handle];
		}
		return isInsideBox(x, y) ? Cursor.MOVE : Cursor.CROSSHAIR;
	}

	private void startSelection(double x, double y) {
		activeSelectionVariant = currentVariant;
		startX = clamp(x, canvasWidth);
		startY = clamp(y, canvasHeight);
		currentX = startX;
		currentY = startY;
		lassoPoints.clear();
		lassoPoints.add(new double[] { startX, startY });
		selecting = true;
		hasSelection = false;
		floating = null;
		mode = Mode.CREATING;
		drawSelection();
	}

	private void updateSelection(double x, double y) {
		if (!selecting) {
			return;
		}
		currentX = clamp(x, canvasWidth);
		currentY = clamp(y, canvasHeight);
		if (activeSelectionVariant == SelectionVariant.FREE) {
			double[] last = lassoPoints.get(lassoPoints.size() - 1);
			if (last[0] != currentX || last[1] != currentY) {
				lassoPoints.add(new double[] { currentX, currentY });
			}
		}
		drawSelection();
	}

	private void endSelection(double x, double y) {
		if (!selecting) {
			return;
		}
		updateSelection(x, y);
		selecting = false;
		mode = Mode.NONE;

		double[] bounds = creationBounds();
		boolean validShape = activeSelectionVariant != SelectionVariant.FREE || lassoPoints.size() >= 3;
		if (!validShape || bounds[2] <= 0 || bounds[3] <= 0) {
			lassoPoints.clear();
			clearOverlay();
			return;
		}

		// Snap to whole pixels so the lifted image lines up with the canvas
		origX = Math.floor(bounds[0]);
		origY = Math.floor(bounds[1]);
		origW = Math.max(1, Math.ceil(bounds[0] + bounds[2]) - origX);
		origH = Math.max(1, Math.ceil(bounds[1] + bounds[3]) - origY);
		boxX = origX;
		boxY = origY;
		boxW = origW;
		boxH = origH;
		hasSelection = true;
		drawEditing();
	}

	private void beginResize(int handle) {
		mode = Mode.RESIZING;
		activeHandle = handle;
		edgeLeft = boxX;
		edgeRight = boxX + boxW;
		edgeTop = boxY;
		edgeBottom = boxY + boxH;
	}

	// Edges can cross each other, which flips which side counts as left/right or top/bottom
	private void resizeTo(double x, double y) {
		if (HANDLE_LEFT[activeHandle]) {
			edgeLeft = x;
		}
		if (HANDLE_RIGHT[activeHandle]) {
			edgeRight = x;
		}
		if (HANDLE_TOP[activeHandle]) {
			edgeTop = y;
		}
		if (HANDLE_BOTTOM[activeHandle]) {
			edgeBottom = y;
		}
		boxX = Math.min(edgeLeft, edgeRight);
		boxY = Math.min(edgeTop, edgeBottom);
		boxW = Math.max(1, Math.abs(edgeRight - edgeLeft));
		boxH = Math.max(1, Math.abs(edgeBottom - edgeTop));
	}

	// Copies the selected pixels into an image and erases them from the canvas (once per selection)
	private void liftSelection() {
		if (floating != null) {
			return;
		}
		SnapshotParameters region = new SnapshotParameters();
		region.setViewport(new Rectangle2D(origX, origY, origW, origH));
		WritableImage source = imageCanvas.snapshot(region, null);

		Canvas temp = new Canvas(origW, origH);
		GraphicsContext tempContext = temp.getGraphicsContext2D();
		if (activeSelectionVariant == SelectionVariant.FREE) {
			tracePath(tempContext, origX, origY, 1, 1, 0, 0, true);
			tempContext.clip();
		}
		tempContext.drawImage(source, 0, 0);
		SnapshotParameters transparent = new SnapshotParameters();
		transparent.setFill(Color.TRANSPARENT);
		floating = temp.snapshot(transparent, null);

		// For now the canvas is white-backed, so the hole left behind is filled white
		imageContext.save();
		if (activeSelectionVariant == SelectionVariant.FREE) {
			tracePath(imageContext, 0, 0, 1, 1, 0, 0, true);
			imageContext.clip();
		}
		imageContext.setFill(Color.WHITE);
		imageContext.fillRect(origX, origY, origW, origH);
		imageContext.restore();
	}

	private int handleAt(double x, double y) {
		double[][] centers = handleCenters();
		for (int i = 0; i < centers.length; i++) {
			if (Math.abs(x - centers[i][0]) <= HANDLE_HIT_RADIUS && Math.abs(y - centers[i][1]) <= HANDLE_HIT_RADIUS) {
				return i;
			}
		}
		return -1;
	}

	private double[][] handleCenters() {
		double midX = boxX + boxW / 2;
		double midY = boxY + boxH / 2;
		double right = boxX + boxW;
		double bottom = boxY + boxH;
		return new double[][] {
			{ boxX, boxY }, { midX, boxY }, { right, boxY }, { right, midY },
			{ right, bottom }, { midX, bottom }, { boxX, bottom }, { boxX, midY }
		};
	}

	private boolean isInsideBox(double x, double y) {
		return x >= boxX && x <= boxX + boxW && y >= boxY && y <= boxY + boxH;
	}

	// Returns {x, y, width, height} of the selection being drawn
	private double[] creationBounds() {
		if (activeSelectionVariant != SelectionVariant.FREE) {
			return new double[] {
				Math.min(startX, currentX), Math.min(startY, currentY),
				Math.abs(currentX - startX), Math.abs(currentY - startY)
			};
		}
		if (lassoPoints.isEmpty()) {
			return new double[] { 0, 0, 0, 0 };
		}
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = 0;
		double maxY = 0;
		for (double[] p : lassoPoints) {
			minX = Math.min(minX, p[0]);
			minY = Math.min(minY, p[1]);
			maxX = Math.max(maxX, p[0]);
			maxY = Math.max(maxY, p[1]);
		}
		return new double[] { minX, minY, maxX - minX, maxY - minY };
	}

	private void applySelectionStyle() {
		context.setStroke(Color.BLACK);
		context.setLineWidth(LINE_WIDTH);
		context.setLineDashes(4, 4);
	}

	// Outline while the selection is still being dragged out
	private void drawSelection() {
		clearOverlay();
		applySelectionStyle();

		if (activeSelectionVariant == SelectionVariant.FREE) {
			if (lassoPoints.size() >= 2) {
				tracePath(context, 0, 0, 1, 1, 0, 0, false);
				context.stroke();
			}
			return;
		}

		double[] b = creationBounds();
		strokeBox(b[0], b[1], b[2], b[3]);
	}

	// Floating pixels, outline and resize handles for a finished selection
	private void drawEditing() {
		clearOverlay();
		if (floating != null) {
			context.drawImage(floating, boxX, boxY, boxW, boxH);
		}

		applySelectionStyle();
		if (activeSelectionVariant == SelectionVariant.FREE) {
			tracePath(context, origX, origY, boxW / origW, boxH / origH, boxX, boxY, true);
			context.stroke();
		} else {
			strokeBox(boxX, boxY, boxW, boxH);
		}

		context.setLineDashes((double[]) null);
		context.setFill(Color.WHITE);
		for (double[] center : handleCenters()) {
			double x = center[0] - HANDLE_SIZE / 2;
			double y = center[1] - HANDLE_SIZE / 2;
			context.fillRect(x, y, HANDLE_SIZE, HANDLE_SIZE);
			context.strokeRect(x, y, HANDLE_SIZE, HANDLE_SIZE);
		}
	}

	private void strokeBox(double x, double y, double width, double height) {
		context.strokeRect(x + LINE_WIDTH / 2, y + LINE_WIDTH / 2,
				Math.max(0, width - LINE_WIDTH), Math.max(0, height - LINE_WIDTH));
	}

	// Builds the lasso path on a context, mapping each point to (point - orig) * scale + box
	private void tracePath(GraphicsContext target, double ox, double oy, double scaleX, double scaleY,
			double bx, double by, boolean close) {
		target.beginPath();
		for (int i = 0; i < lassoPoints.size(); i++) {
			double px = (lassoPoints.get(i)[0] - ox) * scaleX + bx;
			double py = (lassoPoints.get(i)[1] - oy) * scaleY + by;
			if (i == 0) {
				target.moveTo(px, py);
			} else {
				target.lineTo(px, py);
			}
		}
		if (close) {
			target.closePath();
		}
	}

	private void clearOverlay() {
		context.clearRect(0, 0, canvasWidth, canvasHeight);
	}

	private double clamp(double value, double maximum) {
		return Math.clamp(value, 0, maximum);
	}
}

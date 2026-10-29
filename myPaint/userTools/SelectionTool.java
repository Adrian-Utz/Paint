package myPaint.userTools;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/*
Lets the user select part of the canvas.
- FREE: freehand lasso that follows the mouse path and closes when released.
- RECTANGLE: free-size rectangle dragged from the start point.

Written by: AJ Utz
Written on: 8/13/2026
Last Update: 10/29/2026
*/
public class SelectionTool {

	public enum SelectionVariant {
		FREE,
		RECTANGLE
	}

	private static final double LINE_WIDTH = 1.0;

	private final GraphicsContext context;
	private final double canvasWidth;
	private final double canvasHeight;
	private SelectionVariant currentVariant = SelectionVariant.FREE;
	private SelectionVariant activeSelectionVariant = SelectionVariant.FREE;
	private double startX;
	private double startY;
	private double currentX;
	private double currentY;
	private final List<double[]> lassoPoints = new ArrayList<>();
	private boolean selecting;
	private boolean hasSelection;

	public SelectionTool(GraphicsContext context, double canvasWidth, double canvasHeight) {
		if (context == null) {
			throw new IllegalArgumentException("Selection graphics context cannot be null.");
		}
		if (canvasWidth <= 0 || canvasHeight <= 0) {
			throw new IllegalArgumentException("Canvas dimensions must be positive.");
		}
		this.context = context;
		this.canvasWidth = canvasWidth;
		this.canvasHeight = canvasHeight;
		applySelectionStyle();
	}

	public void setSelectionVariant(SelectionVariant variant) {
		if (variant == null) {
			return;
		}
		// A drag in progress keeps the variant it started with
		this.currentVariant = variant;
	}

	// Copy of the lasso outline, in canvas coordinates, for later move/delete features
	public List<double[]> getLassoPoints() {
		return new ArrayList<>(lassoPoints);
	}

	public void startSelection(double x, double y) {
		activeSelectionVariant = currentVariant;
		startX = clamp(x, canvasWidth);
		startY = clamp(y, canvasHeight);
		currentX = startX;
		currentY = startY;
		lassoPoints.clear();
		lassoPoints.add(new double[] { startX, startY });
		selecting = true;
		hasSelection = false;
		drawSelection();
	}

	public void updateSelection(double x, double y) {
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

	public void endSelection(double x, double y) {
		if (!selecting) {
			return;
		}
		updateSelection(x, y);
		selecting = false;
		boolean validShape = activeSelectionVariant != SelectionVariant.FREE || lassoPoints.size() >= 3;
		hasSelection = validShape && getSelectionWidth() > 0 && getSelectionHeight() > 0;
		if (hasSelection) {
			drawSelection();
		} else {
			lassoPoints.clear();
			clearSelection();
		}
	}

	public boolean hasSelection() {
		return hasSelection;
	}


	// Bounding box of the selection (for the lasso, the box around its path)
	public double getSelectionX() {
		return activeSelectionVariant == SelectionVariant.FREE ? lassoBounds()[0] : Math.min(startX, selectionEndX());
	}

	public double getSelectionY() {
		return activeSelectionVariant == SelectionVariant.FREE ? lassoBounds()[1] : Math.min(startY, selectionEndY());
	}

	public double getSelectionWidth() {
		return activeSelectionVariant == SelectionVariant.FREE ? lassoBounds()[2] : Math.abs(selectionEndX() - startX);
	}

	public double getSelectionHeight() {
		return activeSelectionVariant == SelectionVariant.FREE ? lassoBounds()[3] : Math.abs(selectionEndY() - startY);
	}

	// Returns {x, y, width, height} of the lasso path
	private double[] lassoBounds() {
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
		if (lassoPoints.isEmpty()) {
			return new double[] { 0, 0, 0, 0 };
		}
		return new double[] { minX, minY, maxX - minX, maxY - minY };
	}

	private void applySelectionStyle() {
		context.setStroke(Color.BLACK);
		context.setLineWidth(LINE_WIDTH);
		context.setLineDashes(4, 4);
	}

	private void drawSelection() {
		context.clearRect(0, 0, canvasWidth, canvasHeight);
		applySelectionStyle();

		if (activeSelectionVariant == SelectionVariant.FREE) {
			drawLasso();
			return;
		}

		double x1 = startX;
		double y1 = startY;
		double x2 = selectionEndX();
		double y2 = selectionEndY();

		double x = Math.min(x1, x2);
		double y = Math.min(y1, y2);
		double width = Math.abs(x2 - x1);
		double height = Math.abs(y2 - y1);
		context.strokeRect(x + LINE_WIDTH / 2, y + LINE_WIDTH / 2,
				Math.max(0, width - LINE_WIDTH), Math.max(0, height - LINE_WIDTH));
	}

	// Outline follows the mouse path; once the drag ends it is closed back to the start point
	private void drawLasso() {
		int count = lassoPoints.size();
		if (count < 2) {
			return;
		}
		context.beginPath();
		context.moveTo(lassoPoints.get(0)[0], lassoPoints.get(0)[1]);
		for (int i = 1; i < count; i++) {
			context.lineTo(lassoPoints.get(i)[0], lassoPoints.get(i)[1]);
		}
		if (!selecting) {
			context.closePath();
		}
		context.stroke();
	}

    //Reset Selection
	private void clearSelection() {
		context.clearRect(0, 0, canvasWidth, canvasHeight);
	}

	private double clamp(double value, double maximum) {
		return Math.clamp(value, 0, maximum);
	}

	//Return the last known X and Y values 
	private double selectionEndX() {
		return currentX;
	}

	private double selectionEndY() {
		return currentY;
	}
}

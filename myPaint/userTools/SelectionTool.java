package myPaint.userTools;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/*
Lets the user create a rectangular selection on the canvas.

Written by: AJ Utz
Written on: 8/13/2026
Last Update: 10/14/2026
*/
public class SelectionTool {

	public enum SelectionVariant {
		FREE,
		SQUARE
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
		this.currentVariant = variant;
		if (selecting) {
			activeSelectionVariant = variant;
			drawSelection();
		}
	}

	public void startSelection(double x, double y) {
		activeSelectionVariant = currentVariant;
		startX = clamp(x, canvasWidth);
		startY = clamp(y, canvasHeight);
		currentX = startX;
		currentY = startY;
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
		drawSelection();
	}

	public void endSelection(double x, double y) {
		if (!selecting) {
			return;
		}
		updateSelection(x, y);
		selecting = false;
		hasSelection = getSelectionWidth() > 0 && getSelectionHeight() > 0;
		if (!hasSelection) {
			clearSelection();
		}
	}

	public boolean hasSelection() {
		return hasSelection;
	}


	public double getSelectionX() {
		return Math.min(startX, selectionEndX());
	}

	public double getSelectionY() {
		return Math.min(startY, selectionEndY());
	}

	public double getSelectionWidth() {
		return Math.abs(selectionEndX() - startX);
	}

	public double getSelectionHeight() {
		return Math.abs(selectionEndY() - startY);
	}

	private void applySelectionStyle() {
		context.setStroke(Color.BLACK);
		context.setLineWidth(LINE_WIDTH);
		context.setLineDashes(4, 4);
	}

	private void drawSelection() {
		context.clearRect(0, 0, canvasWidth, canvasHeight);
		applySelectionStyle();

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

    //Reset Selection
	private void clearSelection() {
		context.clearRect(0, 0, canvasWidth, canvasHeight);
	}

	private double clamp(double value, double maximum) {
		return Math.clamp(value, 0, maximum);
	}

	//Return the last known X and Y values 
	private double selectionEndX() {
		if (activeSelectionVariant != SelectionVariant.SQUARE) {
			return currentX;
		}
		return startX + squareSide() * directionX();
	}

	private double selectionEndY() {
		if (activeSelectionVariant != SelectionVariant.SQUARE) {
			return currentY;
		}
		return startY + squareSide() * directionY();
	}

	private double squareSide() {
		double side = Math.max(Math.abs(currentX - startX), Math.abs(currentY - startY));
		double availableWidth = directionX() > 0 ? canvasWidth - startX : startX;
		double availableHeight = directionY() > 0 ? canvasHeight - startY : startY;
		return Math.min(side, Math.min(availableWidth, availableHeight));
	}

	private double directionX() {
		double delta = currentX - startX;
		if (delta == 0) {
			return startX <= canvasWidth - startX ? 1 : -1;
		}
		return Math.signum(delta);
	}

	private double directionY() {
		double delta = currentY - startY;
		if (delta == 0) {
			return startY <= canvasHeight - startY ? 1 : -1;
		}
		return Math.signum(delta);
	}
}

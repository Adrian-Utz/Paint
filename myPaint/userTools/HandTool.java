package myPaint.userTools;

import javafx.scene.Node;
import javafx.scene.layout.Region;
/*
Written by: AJ Utz
Written on: 8/13/2026
Last Update: 9/9/2026
*/
public class HandTool {

	private static final double MIN_ZOOM = 0.25;
	private static final double MAX_ZOOM = 4.0;
	private static final double ZOOM_STEP = 1.1; // The factor by which the zoom level changes with each scroll event
    private static final double PAN_SPEED = 1.5;

	private final Node canvasView;
	private final Region viewport;
	private final double contentWidth;
	private final double contentHeight;
	private double zoom = 1.0; // Current zoom level
	private double panStartX;
	private double panStartY;
	private double translationStartX;
	private double translationStartY;

	public HandTool(Node canvasView, Region viewport, double contentWidth, double contentHeight) {
		this.canvasView = canvasView;
		this.viewport = viewport;
		this.contentWidth = contentWidth;
		this.contentHeight = contentHeight;
	}

	public void startPan(double x, double y) {
		panStartX = x;
		panStartY = y;
		translationStartX = canvasView.getTranslateX();
		translationStartY = canvasView.getTranslateY();
	}

	public void panTo(double x, double y) {
		double nextX = translationStartX + (x - panStartX) * PAN_SPEED;
		double nextY = translationStartY + (y - panStartY) * PAN_SPEED;

		canvasView.setTranslateX(clampTranslation(nextX, contentWidth * zoom, viewport.getWidth()));
		canvasView.setTranslateY(clampTranslation(nextY, contentHeight * zoom, viewport.getHeight()));
	}

	public void zoomAt(double scrollAmount) {
		// Calculate the new zoom level based on the scroll amount
		double nextZoom = scrollAmount > 0 ? zoom * ZOOM_STEP : zoom / ZOOM_STEP;
		zoom = Math.clamp(nextZoom, MIN_ZOOM, MAX_ZOOM); //Use Math.clamp for a more readable line
		canvasView.setScaleX(zoom);
		canvasView.setScaleY(zoom);
		canvasView.setTranslateX(clampTranslation(canvasView.getTranslateX(), contentWidth * zoom, viewport.getWidth()));
		canvasView.setTranslateY(clampTranslation(canvasView.getTranslateY(), contentHeight * zoom, viewport.getHeight()));
	}

	// Clamps the translation to ensure the content stays within the viewport bounds
	private double clampTranslation(double translation, double scaledContent, double viewportSize) {
		if (scaledContent <= viewportSize) {
			return 0;
		}

		double maximum = (scaledContent - viewportSize) / 2.0;
		return Math.clamp(translation, -maximum, maximum);
	}
}

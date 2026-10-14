package myPaint.userTools;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/*
This class keeps track of the brush tool.
Current Features:
- Simple drawing
- Adjustable brush color
- Adjustable brush size
- Brush variants
- Palette management

Planned Features:
- Brush variants
- Palette creator

Written by: AJ Utz
Written on: 8/13/2026
Last Update: 9/9/2026
*/
public class BrushTool {

	private static final double DEFAULT_SIZE = 4.0;

	public enum BrushVariant {
		ROUND,
		SQUARE,
		SOFT
	}

	private final GraphicsContext context;
	private Color currentColor;
	private double currentSize;
	private BrushVariant currentVariant;
	private final List<Color> palette;

	//Set the brush's current parameters
	public BrushTool(GraphicsContext context) {
		this.context = context;
		this.currentColor = Color.BLACK;
		this.currentSize = DEFAULT_SIZE;
		this.currentVariant = BrushVariant.ROUND;
		this.palette = new ArrayList<>();
		this.palette.add(currentColor);
		applyBrushStyle();
	}

	public void setColor(Color color) {
		if (color == null) {
			return;
		}
		this.currentColor = color;
		addColorToPalette(color);
		applyBrushStyle();
	}

	public void setBrushSize(double size) {
		this.currentSize = Math.max(1.0, size);
		applyBrushStyle();
	}

	public void setBrushVariant(BrushVariant variant) {
		if (variant == null) {
			return;
		}
		this.currentVariant = variant;
		applyBrushStyle();
	}

	public void addColorToPalette(Color color) {
		if (color == null || palette.contains(color)) {
			return;
		}
		palette.add(color);
	}

	public List<Color> getPalette() {
		return new ArrayList<>(palette);
	}

	public Color getCurrentColor() {
		return currentColor;
	}

	public double getBrushSize() {
		return currentSize;
	}

	public BrushVariant getBrushVariant() {
		return currentVariant;
	}

	private void applyBrushStyle() {
		context.setStroke(currentColor);
		context.setFill(currentColor);
		context.setLineWidth(currentSize);
		context.setLineCap(currentVariant == BrushVariant.SQUARE ? StrokeLineCap.SQUARE : StrokeLineCap.ROUND);
		context.setLineJoin(StrokeLineJoin.ROUND);
	}

    //Calculate starting point 
	public void startStroke(double x, double y) {
		double radius = currentSize / 2.0;
		context.setFill(currentColor);
		if (currentVariant == BrushVariant.SQUARE) {
			context.fillRect(x - radius, y - radius, currentSize, currentSize);
			return;
		}
		context.fillOval(x - radius, y - radius, currentSize, currentSize);
	}

    //Calculate end point
	public void drawTo(double startX, double startY, double endX, double endY) {
		applyBrushStyle();
		if (currentVariant == BrushVariant.SQUARE) {
			// Stamp axis-aligned squares along the path so the square never rotates
			double half = currentSize / 2.0;
			double dx = endX - startX;
			double dy = endY - startY;
			int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy))));
			for (int i = 0; i <= steps; i++) {
				double t = (double) i / steps;
				context.fillRect(startX + dx * t - half, startY + dy * t - half, currentSize, currentSize);
			}
			return;
		}
		if (currentVariant == BrushVariant.SOFT) {
			context.setLineWidth(Math.max(1.0, currentSize * 0.75));
		}
		context.strokeLine(startX, startY, endX, endY);
	}
}

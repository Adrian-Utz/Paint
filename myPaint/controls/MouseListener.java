package myPaint.controls;

import myPaint.userTools.BrushTool;
import myPaint.userTools.HandTool;
import myPaint.userTools.SelectionTool;
import myPaint.utils.ToolType;

import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;

/*
Seperated the Brush and Hand events. The brush uses canvas-local coords, hand uses scene coords from a fixed viewpoint.

Written by: AJ Utz
Written on: 8/13/2026
Last Update: 9/9/2026
*/

public class MouseListener {

	private final BrushTool brushTool;
	private final HandTool handTool;
	private final SelectionTool selectionTool;
	private ToolType activeTool = ToolType.NONE;
	private double lastX;
	private double lastY;

	public MouseListener(BrushTool brushTool, HandTool handTool, SelectionTool selectionTool) {
		this.brushTool = brushTool;
		this.handTool = handTool;
		this.selectionTool= selectionTool;
	}

	// Registers the mouse event handlers for the canvas and viewport
	public void register(Canvas canvas, Region viewport) {
		canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onCanvasPressed);
		canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onCanvasDragged);
		canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::onCanvasReleased);
		viewport.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onViewportPressed);
		viewport.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onViewportDragged);
		viewport.addEventHandler(ScrollEvent.SCROLL, this::onScroll);
	}

	public void setActiveTool(ToolType activeTool) {
		this.activeTool = activeTool;
	}

	
	private void onCanvasPressed(MouseEvent event) {
		// If the active tool is the brush and the primary mouse button is pressed, start a new stroke
		if (activeTool == ToolType.BRUSH && event.getButton() == MouseButton.PRIMARY) {
			lastX = event.getX();
			lastY = event.getY();
			brushTool.startStroke(lastX, lastY);
		} else if (activeTool == ToolType.SELECT && event.getButton() == MouseButton.PRIMARY) {
			selectionTool.startSelection(event.getX(), event.getY());
		}
	}

	private void onCanvasDragged(MouseEvent event) {
		if (!event.isPrimaryButtonDown()) {
			return;
		}

		double currentX = event.getX();
		double currentY = event.getY();
		if (activeTool == ToolType.BRUSH) {
			brushTool.drawTo(lastX, lastY, currentX, currentY);
			lastX = currentX;
			lastY = currentY;
		} else if (activeTool == ToolType.SELECT) {
			selectionTool.updateSelection(currentX, currentY);
		}
	}

	private void onCanvasReleased(MouseEvent event) {
		if (activeTool == ToolType.SELECT && event.getButton() == MouseButton.PRIMARY) {
			selectionTool.endSelection(event.getX(), event.getY());
		}
	}

	private void onViewportPressed(MouseEvent event) {
		// If the active tool is the hand tool and the primary mouse button is pressed, start panning
		if (activeTool == ToolType.HAND && event.getButton() == MouseButton.PRIMARY) {
			handTool.startPan(event.getSceneX(), event.getSceneY());
		}
	}

	private void onViewportDragged(MouseEvent event) {
		//If the active tool is the hand tool and the primary mouse button is pressed, pan to the new position
		if (activeTool == ToolType.HAND && event.isPrimaryButtonDown()) {
			handTool.panTo(event.getSceneX(), event.getSceneY());
		}
	}

	private void onScroll(ScrollEvent event) {
		// If the active tool is the hand tool, zoom in or out based on the scroll amount
		if (activeTool == ToolType.HAND) {
			handTool.zoomAt(event.getDeltaY());
		}
	}
}

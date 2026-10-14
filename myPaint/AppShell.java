package myPaint;

import myPaint.controls.MouseListener;
import myPaint.userTools.BrushTool;
import myPaint.userTools.HandTool;
import myPaint.userTools.SelectionTool;
import myPaint.utils.ToolType;
import myPaint.utils.VersionNumber;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Region;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.Map;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/*
This is the Shell for the paint program. It contains:
- Start: Method to create all UI elements and borders. Call methods in here to see them in the UI.
- createInfoBox: Creates a bar at the top and tells the user info like canvas size.
- createToolSidebar: Creates the sidebar where all the brushes, erasers, and other tools reside.
- selectTool: Each button has a enum in the tooltype file. It will automatically disable any tools untill the button is pressed.
- createDrawingArea: Creates a canvas with the specified width and height. 

Written by: AJ Utz
Written on: 8/13/2026
Last Update: 10/9/2026
*/
public class AppShell extends Application {

    private static final double CANVAS_WIDTH = 1060;
    private static final double CANVAS_HEIGHT = 740;
    private static final String SMALL_LABEL_STYLE = "-fx-font-size: 11px; -fx-text-fill: #374151;";
    private static final String SQUARE_LABEL = "Square";
    private static final String FREE_SELECTION_LABEL = "Free";

    private Stage primaryStage;
    private BrushTool brushTool;
    private SelectionTool selectionTool;
    private MouseListener mouseListener;
    private Label toolStatus;
    private ToolType activeTool = ToolType.NONE;
    private final Map<ToolType, Button> toolButtons = new EnumMap<>(ToolType.class);
    private final Map<ToolType, VBox> settingsPanels = new EnumMap<>(ToolType.class);

    // Here the UI elements are created and added to the scene. The scene is then set to the stage and is shown.
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        BorderPane root = new BorderPane();
        root.setTop(createInfoBar());
        root.setLeft(createToolSidebar());
        root.setCenter(createDrawingArea());

        Scene scene = new Scene(root, 1200, 800);
        scene.setFill(Color.web("#f3f3f3"));

        primaryStage.setTitle("Paint Studio v" + VersionNumber.VERSION_NUMBER);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // This method creates the top bar that displays information about the application, such as the canvas size and the current tool selected.
    private HBox createInfoBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 18, 10, 18));
        bar.setStyle(
            "-fx-background-color: #335555; " +
            "-fx-border-color: #374151; " +
            "-fx-border-width: 0 0 1 0;"
        );

        Label appTitle = new Label("Paint App");
        appTitle.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label canvasStatus = new Label(String.format("Canvas: %.0f x %.0f", CANVAS_WIDTH, CANVAS_HEIGHT));
        canvasStatus.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 13px;");

        toolStatus = new Label("Tool: None");
        toolStatus.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label status = new Label("Ready");
        status.setStyle("-fx-text-fill: #86efac; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        bar.getChildren().addAll(appTitle, canvasStatus, toolStatus, spacer, status);
        return bar;
    }
    
    // This method creates the sidebar that contains all the tools available for the user to use, such as brushes, erasers, and fill tools.
    private VBox createToolSidebar() {
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(16, 12, 16, 12));
        sidebar.setPrefWidth(170);
        sidebar.setStyle(
            "-fx-background-color: #e5e7eb; " +
            "-fx-border-color: #cbd5e1; " +
            "-fx-border-width: 0 1 0 0;"
        );

        Label toolsTitle = new Label("Tools");
        toolsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #111827;");

        VBox toolButtonGroup = new VBox(8);
        toolButtonGroup.setFillWidth(true);

        // Create buttons for each tool type and add them to the sidebar
        for (ToolType tool : ToolType.values()) {
            if (tool == ToolType.NONE) {
                continue;
            }

            Button button = new Button(toolDisplayName(tool));
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            setDefaultButtonStyle(button);
            button.setOnAction(event -> selectTool(tool));

            toolButtons.put(tool, button);
            toolButtonGroup.getChildren().add(button);
        }

        VBox brushSettingsPanel = createBrushSettingsPanel();
        settingsPanels.put(ToolType.BRUSH, brushSettingsPanel);
        hideAllSettings();

        VBox selectionSettingsPanel = createSelectionToolPanel();
        settingsPanels.put(ToolType.SELECT, selectionSettingsPanel);
        hideAllSettings();

        sidebar.getChildren().addAll(toolsTitle, toolButtonGroup, brushSettingsPanel, selectionSettingsPanel);
        return sidebar;
    }

    private VBox createBrushSettingsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle(
            "-fx-background-color: #f3f4f6; " +
            "-fx-border-color: #d1d5db; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8;"
        );

        Label settingsTitle = new Label("Brush Settings");
        settingsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #111827;");

        ColorPicker colorPicker = new ColorPicker(Color.BLACK);
        colorPicker.setOnAction(event -> {
            if (brushTool != null) {
                brushTool.setColor(colorPicker.getValue());
            }
            //Delayed call so the entire application does not slip behind other app windows
            Platform.runLater(() -> {
                if(primaryStage != null){
                    primaryStage.toFront();
                    primaryStage.requestFocus();
                }
            });
        });

        Label sizeLabel = new Label("Size");
        sizeLabel.setStyle(SMALL_LABEL_STYLE);

        Slider sizeSlider = new Slider(1, 50, 4);
        sizeSlider.setPrefWidth(120);
        sizeSlider.setShowTickLabels(false);
        sizeSlider.setMajorTickUnit(10);
        sizeSlider.setMinorTickCount(0);
        sizeSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (brushTool != null) {
                brushTool.setBrushSize(newValue.doubleValue());
            }
        });

        Label variantLabel = new Label("Variant");
        variantLabel.setStyle(SMALL_LABEL_STYLE);

        ComboBox<String> variantBox = new ComboBox<>(
            FXCollections.observableArrayList("Round", SQUARE_LABEL, "Soft")
        );
        variantBox.setValue("Round");
        variantBox.setOnAction(event -> {
            if (brushTool == null) {
                return;
            }
            String selected = variantBox.getValue();
            switch (selected) {
                case SQUARE_LABEL:
                    brushTool.setBrushVariant(BrushTool.BrushVariant.SQUARE);
                    break;
                case "Soft":
                    brushTool.setBrushVariant(BrushTool.BrushVariant.SOFT);
                    break;
                default:
                    brushTool.setBrushVariant(BrushTool.BrushVariant.ROUND);
                    break;
            }
        });

        panel.getChildren().addAll(settingsTitle, colorPicker, sizeLabel, sizeSlider, variantLabel, variantBox);
        return panel;
    }

    private VBox createSelectionToolPanel(){
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle(
            "-fx-background-color: #f3f4f6; " +
            "-fx-border-color: #d1d5db; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8;"
        );

        Label variantLabel = new Label("Variant");
        variantLabel.setStyle(SMALL_LABEL_STYLE);

        ComboBox<String> variantBox = new ComboBox<>(
            FXCollections.observableArrayList(FREE_SELECTION_LABEL, SQUARE_LABEL)
        );
        variantBox.setValue(FREE_SELECTION_LABEL);
        variantBox.setOnAction(event -> {
            if (selectionTool == null) {
                return;
            }
            SelectionTool.SelectionVariant variant = FREE_SELECTION_LABEL.equals(variantBox.getValue())
                ? SelectionTool.SelectionVariant.FREE
                : SelectionTool.SelectionVariant.SQUARE;
            selectionTool.setSelectionVariant(variant);
        });

        panel.getChildren().addAll(variantLabel, variantBox);
        return panel;
    }

    //Shared method handles button behavior. Only one button active at a time, info bar shows enum name.
    private void selectTool(ToolType tool) {
        if (activeTool == tool) {
            activeTool = ToolType.NONE;
            mouseListener.setActiveTool(activeTool);
            toolStatus.setText("Tool: None");
            showAllToolButtons();
            hideAllSettings();
            return;
        }

        activeTool = tool;
        mouseListener.setActiveTool(activeTool);
        toolStatus.setText("Tool: " + activeTool.name());

        showOnlySelectedTool(tool);
        showSettingsForTool(tool);
    }

    // This method updates the visibility and style of the tool buttons in the sidebar, 
    // showing only the button for the selected tool and hiding the others.
    private void showOnlySelectedTool(ToolType selectedTool) {
        for (Map.Entry<ToolType, Button> entry : toolButtons.entrySet()) {
            Button button = entry.getValue();
            boolean selected = entry.getKey() == selectedTool;
            button.setVisible(selected);
            button.setManaged(selected);
            if (selected) {
                setActiveButtonStyle(button);
            } else {
                setDefaultButtonStyle(button);
            }
        }
    }

    // This method resets the visibility and style of all tool buttons in the sidebar, 
    // making them all visible and applying the default style.
    private void showAllToolButtons() {
        for (Button button : toolButtons.values()) {
            button.setVisible(true);
            button.setManaged(true);
            setDefaultButtonStyle(button);
        }
    }

    // This method updates the visibility of the settings panel for the selected tool,
    // showing only the relevant settings and hiding others.
    private void showSettingsForTool(ToolType tool) {
        hideAllSettings();
        VBox toolSettings = settingsPanels.get(tool);
        if (toolSettings != null) {
            toolSettings.setVisible(true);
            toolSettings.setManaged(true);
        }
    }

    // This method hides all settings panels in the sidebar, making them invisible and unmanaged.
    private void hideAllSettings() {
        for (VBox settingsPanel : settingsPanels.values()) {
            settingsPanel.setVisible(false);
            settingsPanel.setManaged(false);
        }
    }

    private String toolDisplayName(ToolType tool) {
        switch (tool) {
            case BRUSH: return "Brush";
            case ERASER: return "Eraser";
            case FILL: return "Fill";
            case HAND: return "Hand";
            case PICKER: return "Picker";
            case SELECT: return "Select";
            default: return tool.name();
        }
    }

    private void setActiveButtonStyle(Button button) {
        button.setStyle(
            "-fx-background-color: #bfdbfe; " +
            "-fx-text-fill: #1e3a8a; " +
            "-fx-border-color: #60a5fa; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6;"
        );
    }

    private void setDefaultButtonStyle(Button button) {
        button.setStyle(
            "-fx-background-color: #ffffff; " +
            "-fx-text-fill: #111827; " +
            "-fx-border-color: #d1d5db; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6;"
        );
    }

    // This method creates the drawing area where the user can draw on the canvas. It sets up a canvas with a white background and registers mouse events for drawing.
    private StackPane createDrawingArea() {
        StackPane drawingArea = new StackPane();
        drawingArea.setPadding(new Insets(20));
        drawingArea.setStyle("-fx-background-color: #f5f3f3;");

        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        GraphicsContext context = canvas.getGraphicsContext2D();
        context.setFill(Color.WHITE);
        context.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        brushTool = new BrushTool(context);

        Canvas selectionCanvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        selectionCanvas.setMouseTransparent(true);
        selectionTool = new SelectionTool(selectionCanvas.getGraphicsContext2D(), CANVAS_WIDTH, CANVAS_HEIGHT);

        StackPane canvasFrame = new StackPane(canvas, selectionCanvas);
        //canvasFrame.setPadding(new Insets(10));
        canvasFrame.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        canvasFrame.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #cbd5e1; " +
            "-fx-border-width: 2; " +
            "-fx-background-radius: 10; " +
            "-fx-border-radius: 10;"
        );

        Rectangle viewportClip = new Rectangle();
        viewportClip.widthProperty().bind(drawingArea.widthProperty());
        viewportClip.heightProperty().bind(drawingArea.heightProperty());
        drawingArea.setClip(viewportClip);

        HandTool handTool = new HandTool(canvasFrame, drawingArea, CANVAS_WIDTH, CANVAS_HEIGHT);
        mouseListener = new MouseListener(brushTool, handTool, selectionTool);
        mouseListener.register(canvas, drawingArea);

        drawingArea.getChildren().add(canvasFrame);
        return drawingArea;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
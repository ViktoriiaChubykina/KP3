package lab.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Rectangle;

import lab.controller.EditorController;
import lab.model.Block;
import lab.model.BlockType;
import lab.model.Flowchart;

import java.util.HashMap;
import java.util.Map;

public final class CanvasView extends Pane {

    private final EditorController editor;
    private final Runnable refreshProps;
    private BlockType addModeKind;
    private final Map<String, StackPane> blockNodes = new HashMap<>();
    private final Pane edgeLayer = new Pane();
    private final Pane nodeLayer = new Pane();
    private String[] highlightPc;

    public CanvasView(EditorController editor, Runnable refreshProps) {
        this.editor = editor;
        this.refreshProps = refreshProps != null ? refreshProps : () -> {};
        getChildren().addAll(edgeLayer, nodeLayer);
        setMinSize(600, 480);
        setStyle("-fx-background-color: #1e1e1e;");

        setOnMouseClicked(ev -> {
            if (ev.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (ev.getTarget() != this && ev.getTarget() != edgeLayer && ev.getTarget() != nodeLayer) {
                return;
            }
            if (addModeKind != null) {
                var b = editor.addBlock(addModeKind, ev.getX() - 75, ev.getY() - 22);
                addModeKind = null;
                editor.setSelectedBlockId(b != null ? b.getId() : null);
                render();
                this.refreshProps.run();
            }
        });
    }

    public void setAddMode(BlockType kind) {
        this.addModeKind = kind;
    }

    public void setHighlightPc(String[] pcs) {
        this.highlightPc = pcs != null ? pcs.clone() : null;
        render();
    }

    public void render() {
        edgeLayer.getChildren().clear();
        nodeLayer.getChildren().clear();
        blockNodes.clear();

        Flowchart fc = editor.activeChart();
        double[] cx = new double[fc.getBlocks().size()];
        double[] cy = new double[fc.getBlocks().size()];
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < fc.getBlocks().size(); i++) {
            Block bk = fc.getBlocks().get(i);
            index.put(bk.getId(), i);
            cx[i] = bk.getLayoutX() + 75;
            cy[i] = bk.getLayoutY() + 22;
        }

        int ti = editor.getActiveThreadIndex();
        for (Block b : fc.getBlocks()) {
            for (var e : b.getOutgoing().entrySet()) {
                String tid = e.getValue();
                if (tid == null || tid.isBlank() || index.get(tid) == null) {
                    continue;
                }
                int ia = index.get(b.getId());
                int ib = index.get(tid);
                var curve = bend(cx[ia], cy[ia], cx[ib], cy[ib], e.getKey());
                curve.setStroke(colorForEdge(e.getKey()));
                curve.setStrokeWidth(1.6);
                curve.setFill(Color.TRANSPARENT);
                edgeLayer.getChildren().add(curve);
            }
        }

        for (Block b : fc.getBlocks()) {
            StackPane box = buildBlockNode(b, ti);
            box.setLayoutX(b.getLayoutX());
            box.setLayoutY(b.getLayoutY());
            nodeLayer.getChildren().add(box);
            blockNodes.put(b.getId(), box);
        }
    }

    private StackPane buildBlockNode(Block b, int threadIdx) {
        Rectangle r = new Rectangle(150, 44);
        boolean sel = b.getId().equals(editor.getSelectedBlockId());
        boolean hi = false;
        if (highlightPc != null && threadIdx < highlightPc.length && b.getId().equals(highlightPc[threadIdx])) {
            hi = true;
        }
        r.setArcWidth(12);
        r.setArcHeight(12);
        r.setFill(Color.web(hi ? "#3d4f2d" : "#2b3038"));
        r.setStroke(Color.web(sel ? "#e0b040" : "#555a60"));
        r.setStrokeWidth(sel ? 2.2 : 1.2);

        Label lb = new Label(b.displayLabel());
        lb.setTextFill(Color.web("#e8e8e8"));
        lb.setStyle("-fx-font-size:11px;");
        lb.setMaxWidth(140);
        lb.setWrapText(true);

        StackPane sp = new StackPane(r, lb);
        StackPane.setAlignment(lb, Pos.CENTER);

        double[] dragAnchor = new double[2];
        sp.setOnMousePressed(me -> {
            if (me.getButton() == MouseButton.PRIMARY) {
                editor.setSelectedBlockId(b.getId());
                dragAnchor[0] = me.getX();
                dragAnchor[1] = me.getY();
                refreshProps.run();
                render();
            }
        });
        sp.setOnMouseDragged(me -> {
            if (me.getButton() == MouseButton.PRIMARY) {
                double nx = sp.getLayoutX() + me.getX() - dragAnchor[0];
                double ny = sp.getLayoutY() + me.getY() - dragAnchor[1];
                editor.moveBlock(b.getId(), Math.max(0, nx), Math.max(0, ny));
                render();
            }
        });
        return sp;
    }

    private static Color colorForEdge(String key) {
        return switch (key) {
            case "true" -> Color.web("#6fbc7a");
            case "false" -> Color.web("#d08080");
            default -> Color.web("#7eb3d8");
        };
    }

    private static CubicCurve bend(double x1, double y1, double x2, double y2, String kind) {
        double dx = (x2 - x1) * 0.45;
        if (Math.abs(dx) < 40) {
            dx = x2 >= x1 ? 40 : -40;
        }
        double c1x = x1 + dx;
        double c1y = y1;
        double c2x = x2 - dx;
        double c2y = y2;
        if ("true".equals(kind) || "false".equals(kind)) {
            c1y += "true".equals(kind) ? -35 : 35;
            c2y += "true".equals(kind) ? -35 : 35;
        }
        return new CubicCurve(x1, y1, c1x, c1y, c2x, c2y, x2, y2);
    }
}

package com.ss.editor.ui.component.editor.impl.scene;

import static com.ss.editor.part3d.editor.impl.scene.AbstractSceneEditor3DPart.LOADED_MODEL_KEY;
import static com.ss.editor.util.EditorUtil.*;
import static com.ss.editor.util.MaterialUtils.saveIfNeedTextures;
import static com.ss.editor.util.MaterialUtils.updateMaterialIdNeed;
import static com.ss.editor.util.NodeUtils.findParent;
import static com.ss.rlib.util.ClassUtils.unsafeCast;
import static com.ss.rlib.util.ObjectUtils.notNull;
import static javafx.collections.FXCollections.observableArrayList;
import com.jme3.asset.AssetManager;
import com.jme3.asset.MaterialKey;
import com.jme3.asset.ModelKey;
import com.jme3.audio.AudioNode;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.AssetLinkNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.texture.Texture;
import com.ss.editor.FileExtensions;
import com.ss.editor.Messages;
import com.ss.editor.annotation.BackgroundThread;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.annotation.JmeThread;
import com.ss.editor.control.transform.EditorTransformSupport.TransformType;
import com.ss.editor.control.transform.EditorTransformSupport.TransformationMode;
import com.ss.editor.extension.scene.SceneLayer;
import com.ss.editor.extension.scene.ScenePresentable;
import com.ss.editor.model.editor.ModelEditingProvider;
import com.ss.editor.model.scene.EditorAudioNode;
import com.ss.editor.model.scene.EditorLightNode;
import com.ss.editor.model.scene.EditorPresentableNode;
import com.ss.editor.model.scene.WrapperNode;
import com.ss.editor.model.undo.editor.ChangeConsumer;
import com.ss.editor.model.undo.editor.ModelChangeConsumer;
import com.ss.editor.part3d.editor.impl.Stats3DPart;
import com.ss.editor.part3d.editor.impl.scene.AbstractSceneEditor3DPart;
import com.ss.editor.plugin.api.RenderFilterExtension;
import com.ss.editor.plugin.api.editor.Advanced3DFileEditorWithSplitRightTool;
import com.ss.editor.ui.Icons;
import com.ss.editor.ui.component.editor.scripting.EditorScriptingComponent;
import com.ss.editor.ui.component.editor.state.impl.BaseEditorSceneEditorState;
import com.ss.editor.ui.component.painting.PaintingComponent;
import com.ss.editor.ui.component.painting.PaintingComponentContainer;
import com.ss.editor.ui.component.tab.EditorToolComponent;
import com.ss.editor.ui.control.model.ModelNodeTree;
import com.ss.editor.ui.control.model.ModelPropertyEditor;
import com.ss.editor.ui.control.property.operation.PropertyOperation;
import com.ss.editor.ui.control.tree.action.impl.operation.AddChildOperation;
import com.ss.editor.ui.control.tree.action.impl.operation.RemoveChildOperation;
import com.ss.editor.ui.control.tree.action.impl.operation.RemoveControlOperation;
import com.ss.editor.ui.control.tree.action.impl.operation.RemoveLightOperation;
import com.ss.editor.ui.control.tree.node.TreeNode;
import com.ss.editor.ui.css.CssClasses;
import com.ss.editor.ui.event.impl.FileChangedEvent;
import com.ss.editor.ui.util.DynamicIconSupport;
import com.ss.editor.ui.util.UiUtils;
import com.ss.editor.util.EditorUtil;
import com.ss.editor.util.LocalObjects;
import com.ss.editor.util.MaterialUtils;
import com.ss.editor.util.NodeUtils;
import com.ss.rlib.ui.util.FXUtils;
import com.ss.rlib.util.FileUtils;
import com.ss.rlib.util.array.Array;
import com.ss.rlib.util.array.ArrayFactory;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * The base implementation of a model file editor.
 *
 * @param <M>  the type edited object.
 * @param <MA> the type of {@link AbstractSceneEditor3DPart}
 * @param <ES> the type of an editor state.
 * @author JavaSaBr
 */
public abstract class AbstractSceneFileEditor<M extends Spatial, MA extends AbstractSceneEditor3DPart, ES extends BaseEditorSceneEditorState> extends
        Advanced3DFileEditorWithSplitRightTool<MA, ES> implements ModelChangeConsumer, ModelEditingProvider {

    private static final int OBJECTS_TOOL = 0;
    private static final int PAINTING_TOOL = 1;

    @NotNull
    private static final Array<String> ACCEPTED_FILES = ArrayFactory.asArray(
            FileExtensions.JME_MATERIAL,
            FileExtensions.JME_OBJECT);

    @NotNull
    private static final ObservableList<TransformationMode> TRANSFORMATION_MODES = observableArrayList(TransformationMode.values());

    /**
     * The list of pre-save handlers.
     */
    @NotNull
    private static final Array<Consumer<Spatial>> PRE_SAVE_HANDLERS = ArrayFactory.newArray(Consumer.class);

    /**
     * The list of post-save handlers.
     */
    @NotNull
    private static final Array<Consumer<Spatial>> POST_SAVE_HANDLERS = ArrayFactory.newArray(Consumer.class);

    /**
     * Register the new pre-save handler.
     *
     * @param handler the new pre-save handler.
     */
    @FxThread
    public static void registerPreSaveHandler(@NotNull Consumer<Spatial> handler) {
        PRE_SAVE_HANDLERS.add(handler);
    }

    /**
     * Register the new post-save handler.
     *
     * @param handler the new post-save handler.
     */
    @FxThread
    public static void registerPostSaveHandler(@NotNull Consumer<Spatial> handler) {
        PRE_SAVE_HANDLERS.add(handler);
    }

    /**
     * The stats 3D part.
     */
    @NotNull
    private final Stats3DPart stats3DPart;

    /**
     * The opened model.
     */
    @Nullable
    private M currentModel;

    /**
     * The selection handler.
     */
    @Nullable
    private Consumer<Object> selectionNodeHandler;

    /**
     * The list of transform modes.
     */
    @Nullable
    private ComboBox<TransformationMode> transformModeComboBox;

    /**
     * The model tree.
     */
    @Nullable
    private ModelNodeTree modelNodeTree;

    /**
     * The model property editor.
     */
    @Nullable
    private ModelPropertyEditor modelPropertyEditor;

    /**
     * The container of painting components.
     */
    @Nullable
    private PaintingComponentContainer paintingComponentContainer;

    /**
     * The scripting component.
     */
    @Nullable
    private EditorScriptingComponent scriptingComponent;

    /**
     * The container of property editor in objects tool.
     */
    @Nullable
    private VBox propertyEditorObjectsContainer;

    /**
     * The container of model node tree in objects tool.
     */
    @Nullable
    private VBox modelNodeTreeObjectsContainer;

    /**
     * The container of model node tree in editing tool.
     */
    @Nullable
    private VBox modelNodeTreeEditingContainer;

    /**
     * The stats container.
     */
    @Nullable
    private VBox statsContainer;

    /**
     * The selection toggle.
     */
    @Nullable
    private ToggleButton selectionButton;

    /**
     * The grid toggle.
     */
    @Nullable
    private ToggleButton gridButton;

    /**
     * The statistics toggle.
     */
    @Nullable
    private ToggleButton statisticsButton;

    /**
     * The move tool toggle.
     */
    @Nullable
    private ToggleButton moveToolButton;

    /**
     * The rotation tool toggle.
     */
    @Nullable
    private ToggleButton rotationToolButton;

    /**
     * The scaling tool toggle.
     */
    @Nullable
    private ToggleButton scaleToolButton;

    /**
     * The flag of ignoring camera moving.
     */
    private boolean ignoreCameraMove;

    public AbstractSceneFileEditor() {
        this.stats3DPart = new Stats3DPart(notNull(statsContainer));
        addEditor3DPart(stats3DPart);
        stats3DPart.setEnabled(true);
        processChangeTool(-1, OBJECTS_TOOL);
    }

    /**
     * Gets model node tree.
     *
     * @return the model tree.
     */
    @FxThread
    protected @NotNull ModelNodeTree getModelNodeTree() {
        return notNull(modelNodeTree);
    }

    /**
     * Gets model property editor.
     *
     * @return the model property editor.
     */
    @FxThread
    protected @NotNull ModelPropertyEditor getModelPropertyEditor() {
        return notNull(modelPropertyEditor);
    }

    /**
     * Get the container of painting components.
     *
     * @return the container of painting components.
     */
    @FxThread
    private @NotNull PaintingComponentContainer getPaintingComponentContainer() {
        return notNull(paintingComponentContainer);
    }

    /**
     * @return the container of property editor in objects tool.
     */
    @FxThread
    private @NotNull VBox getPropertyEditorObjectsContainer() {
        return notNull(propertyEditorObjectsContainer);
    }

    /**
     * @return the container of model node tree in editing tool.
     */
    @FxThread
    private @NotNull VBox getModelNodeTreeEditingContainer() {
        return notNull(modelNodeTreeEditingContainer);
    }

    /**
     * @return the container of model node tree in objects tool.
     */
    @FxThread
    private @NotNull VBox getModelNodeTreeObjectsContainer() {
        return notNull(modelNodeTreeObjectsContainer);
    }

    /**
     * @return the scripting component.
     */
    @FxThread
    private @NotNull EditorScriptingComponent getScriptingComponent() {
        return notNull(scriptingComponent);
    }

    @Override
    @FxThread
    protected void processChangedFile(@NotNull final FileChangedEvent event) {
        super.processChangedFile(event);

        final Path file = event.getFile();
        final String extension = FileUtils.getExtension(file);

        if (extension.endsWith(FileExtensions.JME_MATERIAL)) {
            EXECUTOR_MANAGER.addJmeTask(() -> updateMaterial(file));
        } else if (MaterialUtils.isShaderFile(file) || MaterialUtils.isTextureFile(file)) {
            EXECUTOR_MANAGER.addJmeTask(() -> updateMaterials(file));
        }
    }

    /**
     * Updating a material from the file.
     */
    @FxThread
    private void updateMaterial(@NotNull final Path file) {

        final Path assetFile = notNull(getAssetFile(file), "Not found asset file for " + file);
        final String assetPath = toAssetPath(assetFile);

        final M currentModel = getCurrentModel();

        final Array<Geometry> geometries = ArrayFactory.newArray(Geometry.class);
        NodeUtils.addGeometryWithMaterial(currentModel, geometries, assetPath);
        if (geometries.isEmpty()) return;

        final AssetManager assetManager = EditorUtil.getAssetManager();
        final Material material = assetManager.loadMaterial(assetPath);
        geometries.forEach(geometry -> geometry.setMaterial(material));

        final RenderFilterExtension filterExtension = RenderFilterExtension.getInstance();
        filterExtension.refreshFilters();
    }

    /**
     * Updating materials.
     */
    @FxThread
    private void updateMaterials(@NotNull final Path file) {

        final M currentModel = getCurrentModel();
        final AtomicInteger needRefresh = new AtomicInteger();

        NodeUtils.visitGeometry(currentModel, geometry -> {

            final Material material = geometry.getMaterial();
            final Material newMaterial = updateMaterialIdNeed(file, material);

            if (newMaterial != null) {
                geometry.setMaterial(newMaterial);
                needRefresh.incrementAndGet();
            }
        });

        if (needRefresh.get() < 1) {
            return;
        }

        final RenderFilterExtension filterExtension = RenderFilterExtension.getInstance();
        filterExtension.refreshFilters();
    }

    /**
     * Handle a added model.
     *
     * @param model the model
     */
    @FxThread
    protected void handleAddedObject(@NotNull final Spatial model) {

        final MA editor3DState = getEditor3DPart();
        final Array<Light> lights = ArrayFactory.newArray(Light.class);
        final Array<AudioNode> audioNodes = ArrayFactory.newArray(AudioNode.class);

        NodeUtils.addLight(model, lights);
        NodeUtils.addAudioNodes(model, audioNodes);

        lights.forEach(editor3DState, (light, state) -> state.addLight(light));
        audioNodes.forEach(editor3DState, (audioNode, state) -> state.addAudioNode(audioNode));
    }

    /**
     * Handle a removed model.T
     *
     * @param model the model
     */
    @FxThread
    protected void handleRemovedObject(@NotNull final Spatial model) {

        final MA editor3DState = getEditor3DPart();
        final Array<Light> lights = ArrayFactory.newArray(Light.class);
        final Array<AudioNode> audioNodes = ArrayFactory.newArray(AudioNode.class);

        NodeUtils.addLight(model, lights);
        NodeUtils.addAudioNodes(model, audioNodes);

        lights.forEach(editor3DState, (light, state) -> state.removeLight(light));
        audioNodes.forEach(editor3DState, (audioNode, state) -> state.removeAudioNode(audioNode));
    }

    @Override
    @FxThread
    protected void loadState() {
        super.loadState();

        scriptingComponent.addVariable("root", getCurrentModel());
        scriptingComponent.addVariable("assetManager", EditorUtil.getAssetManager());
        scriptingComponent.addImport(Spatial.class);
        scriptingComponent.addImport(Geometry.class);
        scriptingComponent.addImport(Control.class);
        scriptingComponent.addImport(Node.class);
        scriptingComponent.addImport(Light.class);
        scriptingComponent.addImport(DirectionalLight.class);
        scriptingComponent.addImport(PointLight.class);
        scriptingComponent.addImport(SpotLight.class);
        scriptingComponent.addImport(Material.class);
        scriptingComponent.addImport(Texture.class);
        scriptingComponent.setExampleCode("root.attachChild(\nnew Node(\"created from Groovy\"));");
        scriptingComponent.buildHeader();

        final ES editorState = notNull(getEditorState());

        gridButton.setSelected(editorState.isEnableGrid());
        statisticsButton.setSelected(editorState.isShowStatistics());
        selectionButton.setSelected(editorState.isEnableSelection());
        transformModeComboBox.getSelectionModel()
                .select(TransformationMode.valueOf(editorState.getTransformationMode()));

        final Array<PaintingComponent> components = paintingComponentContainer.getComponents();
        components.forEach(editorState, PaintingComponent::loadState);

        final TransformType transformType = TransformType.valueOf(editorState.getTransformationType());

        switch (transformType) {
            case MOVE_TOOL: {
                moveToolButton.setSelected(true);
                break;
            }
            case ROTATE_TOOL: {
                rotationToolButton.setSelected(true);
                break;
            }
            case SCALE_TOOL: {
                scaleToolButton.setSelected(true);
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    @FxThread
    protected boolean handleKeyActionImpl(@NotNull final KeyCode keyCode, final boolean isPressed,
                                          final boolean isControlDown, final boolean isShiftDown,
                                          final boolean isButtonMiddleDown) {

        final MA editor3DState = getEditor3DPart();
        if (editor3DState.isCameraMoving()) {
            return false;
        }

        if (isPressed && isControlDown && keyCode == KeyCode.Z) {
            undo();
            return true;
        } else if (isPressed && isControlDown && keyCode == KeyCode.Y) {
            redo();
            return true;
        } else if (isPressed && keyCode == KeyCode.G && !isControlDown && !isButtonMiddleDown) {
            final ToggleButton moveToolButton = getMoveToolButton();
            moveToolButton.setSelected(true);
            return true;
        } else if (isPressed && keyCode == KeyCode.R && !isControlDown && !isButtonMiddleDown) {
            final ToggleButton rotationToolButton = getRotationToolButton();
            rotationToolButton.setSelected(true);
            return true;
        } else if (isPressed && keyCode == KeyCode.S && !isControlDown && !isButtonMiddleDown) {
            final ToggleButton scaleToolButton = getScaleToolButton();
            scaleToolButton.setSelected(true);
            return true;
        } else if (isPressed && keyCode == KeyCode.DELETE) {

            final ModelNodeTree modelNodeTree = getModelNodeTree();
            final TreeNode<?> selected = modelNodeTree.getSelected();
            if (selected == null || !selected.canRemove()) return false;

            final Object element = selected.getElement();
            final TreeNode<?> parent = selected.getParent();
            final Object parentElement = parent == null ? null : parent.getElement();

            if (element instanceof Spatial) {
                final Spatial spatial = (Spatial) element;
                execute(new RemoveChildOperation(spatial, spatial.getParent()));
            } else if (element instanceof Light && parentElement instanceof Node) {
                final Light light = (Light) element;
                execute(new RemoveLightOperation(light, (Node) parentElement));
            } else if (element instanceof Control && parentElement instanceof Spatial) {
                final Control control = (Control) element;
                execute(new RemoveControlOperation(control, (Spatial) parentElement));
            }

            return true;
        }

        return super.handleKeyActionImpl(keyCode, isPressed, isControlDown, isShiftDown, isButtonMiddleDown);
    }

    /**
     * @return true if need to ignore moving camera.
     */
    @FxThread
    private boolean isIgnoreCameraMove() {
        return ignoreCameraMove;
    }

    /**
     * @param ignoreCameraMove true if need to ignore moving camera.
     */
    @FxThread
    private void setIgnoreCameraMove(final boolean ignoreCameraMove) {
        this.ignoreCameraMove = ignoreCameraMove;
    }

    /**
     * Sets current model.
     *
     * @param currentModel the opened model.
     */
    @FxThread
    protected void setCurrentModel(@NotNull final M currentModel) {
        this.currentModel = currentModel;
    }

    @Override
    @FxThread
    public @NotNull M getCurrentModel() {
        return notNull(currentModel);
    }

    @Override
    @FxThread
    public void notifyFxChangeProperty(@Nullable final Object parent, @NotNull final Object object,
                                       @NotNull final String propertyName) {

        final ModelPropertyEditor modelPropertyEditor = getModelPropertyEditor();
        modelPropertyEditor.syncFor(object);

        final ModelNodeTree modelNodeTree = getModelNodeTree();
        modelNodeTree.notifyChanged(parent, object);

        if (object instanceof Geometry && Messages.MODEL_PROPERTY_MATERIAL.equals(propertyName)) {
            modelNodeTree.refresh(object);
        }

        final PaintingComponentContainer editingComponentContainer = getPaintingComponentContainer();
        editingComponentContainer.notifyChangeProperty(object, propertyName);
    }

    @Override
    @FxThread
    public void notifyJmeChangeProperty(@NotNull final Object object, @NotNull final String propertyName) {
        getEditor3DPart().notifyPropertyChanged(object);
    }

    @Override
    @FxThread
    public void notifyFxChangePropertyCount(@NotNull final Object object) {
        final ModelPropertyEditor modelPropertyEditor = getModelPropertyEditor();
        modelPropertyEditor.rebuildFor(object, null);
    }

    @Override
    @FxThread
    public void notifyFxAddedChild(@NotNull final Object parent, @NotNull final Object added, final int index,
                                   final boolean needSelect) {

        final MA editor3DPart = getEditor3DPart();
        final ModelNodeTree modelNodeTree = getModelNodeTree();
        modelNodeTree.notifyAdded(parent, added, index);

        if (added instanceof Light) {
            editor3DPart.addLight((Light) added);
        } else if (added instanceof AudioNode) {
            editor3DPart.addAudioNode((AudioNode) added);
        } else if (added instanceof Spatial) {
            handleAddedObject((Spatial) added);
        }

        if (needSelect) {
            EXECUTOR_MANAGER.addJmeTask(() -> EXECUTOR_MANAGER.addFxTask(() -> modelNodeTree.select(added)));
        }
    }

    @Override
    @FxThread
    public void notifyFxRemovedChild(@NotNull final Object parent, @NotNull final Object removed) {

        final MA editor3DState = getEditor3DPart();
        final ModelNodeTree modelNodeTree = getModelNodeTree();
        modelNodeTree.notifyRemoved(parent, removed);

        if (removed instanceof Light) {
            editor3DState.removeLight((Light) removed);
        } else if (removed instanceof AudioNode) {
            editor3DState.removeAudioNode((AudioNode) removed);
        } else if (removed instanceof Spatial) {
            handleRemovedObject((Spatial) removed);
        }
    }

    @Override
    @FxThread
    public void notifyFxReplaced(@NotNull final Object parent, @Nullable final Object oldChild,
                                 @Nullable final Object newChild, final boolean needExpand,
                                 final boolean needDeepExpand) {

        final MA editor3DPart = getEditor3DPart();
        final Spatial currentModel = getCurrentModel();

        if (currentModel == oldChild && newChild instanceof Spatial) {
            handleRemovedObject(currentModel);
            editor3DPart.openModel(unsafeCast(newChild));
            handleAddedObject((Spatial) newChild);
            setCurrentModel(unsafeCast(newChild));
        } else {

            if (oldChild instanceof Spatial) {
                handleRemovedObject((Spatial) oldChild);
            }

            if (newChild instanceof Spatial) {
                handleAddedObject((Spatial) newChild);
            }
        }

        final ModelNodeTree modelNodeTree = getModelNodeTree();
        modelNodeTree.notifyReplace(parent, oldChild, newChild, needExpand, needDeepExpand);
    }

    @Override
    @FxThread
    public void notifyFxMoved(@NotNull final Object prevParent, @NotNull final Object newParent,
                              @NotNull final Object child, final int index, final boolean needSelect) {

        final ModelNodeTree modelNodeTree = getModelNodeTree();
        modelNodeTree.notifyMoved(prevParent, newParent, child, index);

        if (needSelect) {
            EXECUTOR_MANAGER.addJmeTask(() -> EXECUTOR_MANAGER.addFxTask(() -> modelNodeTree.select(child)));
        }
    }

    /**
     * Handle the selected object.
     *
     * @param object the object
     */
    @FxThread
    public void notifySelected(@Nullable Object object) {

        if (object instanceof EditorLightNode) {
            object = ((EditorLightNode) object).getLight();
        }

        if (object instanceof EditorAudioNode) {
            object = ((EditorAudioNode) object).getAudioNode();
        }

        setIgnoreCameraMove(true);
        try {
            final ModelNodeTree modelNodeTree = getModelNodeTree();
            modelNodeTree.select(object);
        } finally {
            setIgnoreCameraMove(false);
        }
    }

    /**
     * Handle the selected object from the Tree.
     *
     * @param object the object
     */
    @FxThread
    public void selectNodeFromTree(@Nullable final Object object) {

        final MA editor3DPart = getEditor3DPart();

        Object parent = null;
        Object element;

        if (object instanceof TreeNode<?>) {
            final TreeNode treeNode = (TreeNode) object;
            final TreeNode parentNode = treeNode.getParent();
            parent = parentNode == null ? null : parentNode.getElement();
            element = treeNode.getElement();
        } else {
            element = object;
        }

        if (element instanceof SceneLayer) {
            element = null;
        }

        Spatial spatial = null;

        if (element instanceof AudioNode) {
            final EditorAudioNode audioNode = editor3DPart.getAudioNode((AudioNode) element);
            spatial = audioNode == null ? null : audioNode.getEditedNode();
        } else if (element instanceof Spatial) {
            spatial = (Spatial) element;
            parent = spatial.getParent();
        } else if (element instanceof Light) {
            spatial = editor3DPart.getLightNode((Light) element);
        } else if(object instanceof ScenePresentable) {
            final EditorPresentableNode presentableNode = editor3DPart.getPresentableNode((ScenePresentable) object);
            spatial = presentableNode == null? null : presentableNode.getEditedNode();
        }

        if (spatial != null && !spatial.isVisible()) {
            spatial = null;
        }

        updateSelection(spatial);

        if (spatial != null && !isIgnoreCameraMove() && !isVisibleOnEditor(spatial)) {
            editor3DPart.cameraLookAt(spatial.getWorldTranslation());
        }

        final ModelPropertyEditor modelPropertyEditor = getModelPropertyEditor();
        modelPropertyEditor.buildFor(element, parent);

        final PaintingComponentContainer editingComponentContainer = getPaintingComponentContainer();
        editingComponentContainer.prepareFor(element);
    }

    @FxThread
    private boolean isVisibleOnEditor(@NotNull final Spatial spatial) {

        final MA editor3DPart = getEditor3DPart();
        final Camera camera = editor3DPart.getCamera();

        final Vector3f position = spatial.getWorldTranslation();
        final Vector3f coordinates = camera.getScreenCoordinates(position, new Vector3f());

        boolean invisible = coordinates.getZ() < 0F || coordinates.getZ() > 1F;
        invisible = invisible || !isInside(coordinates.getX(), camera.getHeight() - coordinates.getY(), Event.class);

        return !invisible;
    }

    /**
     * Update selection to 3D state.
     *
     * @param spatial the new selected object.
     */
    @FxThread
    protected void updateSelection(@Nullable final Spatial spatial) {

        final Array<Spatial> selection = ArrayFactory.newArray(Spatial.class);
        if (spatial != null) selection.add(spatial);

        final MA editor3DState = getEditor3DPart();
        editor3DState.updateSelection(selection);
    }

    @Override
    @BackgroundThread
    public void doSave(@NotNull final Path toStore) throws IOException {
        super.doSave(toStore);

        final M currentModel = getCurrentModel();

        NodeUtils.visitGeometry(currentModel, geometry -> saveIfNeedTextures(geometry.getMaterial()));

        PRE_SAVE_HANDLERS.forEach(currentModel, Consumer::accept);
        try {

            final BinaryExporter exporter = BinaryExporter.getInstance();

            try (final OutputStream out = Files.newOutputStream(toStore)) {
                exporter.save(currentModel, out);
            }

        } finally {
            POST_SAVE_HANDLERS.forEach(currentModel, Consumer::accept);
        }
    }

    @Override
    @FxThread
    protected boolean needToolbar() {
        return true;
    }

    /**
     * @return the scaling tool toggle.
     */
    @FxThread
    private @NotNull ToggleButton getScaleToolButton() {
        return notNull(scaleToolButton);
    }

    /**
     * @return the move tool toggle.
     */
    @FxThread
    private @NotNull ToggleButton getMoveToolButton() {
        return notNull(moveToolButton);
    }

    /**
     * @return the rotation tool toggle.
     */
    @FxThread
    private @NotNull ToggleButton getRotationToolButton() {
        return notNull(rotationToolButton);
    }

    /**
     * Switch transformation mode.
     */
    @FxThread
    private void changeTransformMode(@NotNull final TransformationMode transformationMode) {

        final MA editor3DPart = getEditor3DPart();
        editor3DPart.setTransformMode(transformationMode);

        final ES editorState = getEditorState();

        if (editorState != null) {
            editorState.setTransformationMode(transformationMode.ordinal());
        }
    }

    /**
     * Switch transformation type.
     */
    @FxThread
    private void updateTransformTool(@NotNull final TransformType transformType, @NotNull final Boolean newValue) {

        final MA editor3DPart = getEditor3DPart();
        final ToggleButton scaleToolButton = getScaleToolButton();
        final ToggleButton moveToolButton = getMoveToolButton();
        final ToggleButton rotationToolButton = getRotationToolButton();

        if (newValue != Boolean.TRUE) {
            if (editor3DPart.getTransformType() == transformType) {
                if (transformType == TransformType.MOVE_TOOL) {
                    moveToolButton.setSelected(true);
                } else if (transformType == TransformType.ROTATE_TOOL) {
                    rotationToolButton.setSelected(true);
                } else if (transformType == TransformType.SCALE_TOOL) {
                    scaleToolButton.setSelected(true);
                }
            }
            return;
        }

        final ES editorState = getEditorState();
        editor3DPart.setTransformType(transformType);

        if (transformType == TransformType.MOVE_TOOL) {
            rotationToolButton.setSelected(false);
            scaleToolButton.setSelected(false);
        } else if (transformType == TransformType.ROTATE_TOOL) {
            moveToolButton.setSelected(false);
            scaleToolButton.setSelected(false);
        } else if (transformType == TransformType.SCALE_TOOL) {
            rotationToolButton.setSelected(false);
            moveToolButton.setSelected(false);
        }

        if (editorState != null) {
            editorState.setTransformationType(transformType.ordinal());
        }
    }

    @Override
    @FxThread
    protected void createToolbar(@NotNull final HBox container) {
        createActions(container);

        final Label transformModeLabel = new Label(Messages.MODEL_FILE_EDITOR_TRANSFORM_MODE + ":");

        transformModeComboBox = new ComboBox<>(TRANSFORMATION_MODES);
        transformModeComboBox.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> changeTransformMode(newValue));

        FXUtils.addToPane(transformModeLabel, container);
        FXUtils.addToPane(transformModeComboBox, container);
    }

    @FxThread
    protected void createActions(@NotNull final HBox container) {
        FXUtils.addToPane(createSaveAction(), container);

        selectionButton = new ToggleButton();
        selectionButton.setTooltip(new Tooltip(Messages.SCENE_FILE_EDITOR_ACTION_SELECTION));
        selectionButton.setGraphic(new ImageView(Icons.CUBE_16));
        selectionButton.setSelected(true);
        selectionButton.selectedProperty().addListener((observable, oldValue, newValue) ->
                changeSelectionVisible(newValue));

        gridButton = new ToggleButton();
        gridButton.setTooltip(new Tooltip(Messages.SCENE_FILE_EDITOR_ACTION_GRID));
        gridButton.setGraphic(new ImageView(Icons.PLANE_16));
        gridButton.setSelected(true);
        gridButton.selectedProperty().addListener((observable, oldValue, newValue) ->
                changeGridVisible(newValue));

        statisticsButton = new ToggleButton();
        statisticsButton.setTooltip(new Tooltip(Messages.SCENE_FILE_EDITOR_ACTION_STATISTICS));
        statisticsButton.setGraphic(new ImageView(Icons.STATISTICS_16));
        statisticsButton.setSelected(true);
        statisticsButton.selectedProperty().addListener((observable, oldValue, newValue) ->
                changeStatisticsVisible(newValue));

        moveToolButton = new ToggleButton();
        moveToolButton.setTooltip(new Tooltip(Messages.SCENE_FILE_EDITOR_ACTION_MOVE_TOOL + " (G)"));
        moveToolButton.setGraphic(new ImageView(Icons.MOVE_16));
        moveToolButton.setSelected(true);
        moveToolButton.selectedProperty().addListener((observable, oldValue, newValue) ->
                updateTransformTool(TransformType.MOVE_TOOL, newValue));

        rotationToolButton = new ToggleButton();
        rotationToolButton.setTooltip(new Tooltip(Messages.SCENE_FILE_EDITOR_ACTION_ROTATION_TOOL + " (R)"));
        rotationToolButton.setGraphic(new ImageView(Icons.ROTATION_16));
        rotationToolButton.selectedProperty().addListener((observable, oldValue, newValue) ->
                updateTransformTool(TransformType.ROTATE_TOOL, newValue));

        scaleToolButton = new ToggleButton();
        scaleToolButton.setTooltip(new Tooltip(Messages.SCENE_FILE_EDITOR_ACTION_SCALE_TOOL + " (S)"));
        scaleToolButton.setGraphic(new ImageView(Icons.SCALE_16));
        scaleToolButton.selectedProperty().addListener((observable, oldValue, newValue) ->
                updateTransformTool(TransformType.SCALE_TOOL, newValue));

        DynamicIconSupport.addSupport(selectionButton, gridButton, statisticsButton, moveToolButton, rotationToolButton,
                scaleToolButton);

        FXUtils.addClassesTo(selectionButton, gridButton, statisticsButton, moveToolButton, rotationToolButton,
                scaleToolButton, CssClasses.FILE_EDITOR_TOOLBAR_BUTTON);

        FXUtils.addToPane(selectionButton, container);
        FXUtils.addToPane(gridButton, container);
        FXUtils.addToPane(statisticsButton, container);
        FXUtils.addToPane(moveToolButton, container);
        FXUtils.addToPane(rotationToolButton, container);
        FXUtils.addToPane(scaleToolButton, container);
    }

    @Override
    @FxThread
    protected void createContent(@NotNull final StackPane root) {
        this.selectionNodeHandler = this::selectNodeFromTree;

        propertyEditorObjectsContainer = new VBox();
        modelNodeTreeEditingContainer = new VBox();
        modelNodeTreeObjectsContainer = new VBox();

        paintingComponentContainer = new PaintingComponentContainer(this, this);

        scriptingComponent = new EditorScriptingComponent(this::refreshTree);
        scriptingComponent.prefHeightProperty().bind(root.heightProperty());

        super.createContent(root);

        final StackPane editorAreaPane = getEditorAreaPane();

        statsContainer = new VBox();
        statsContainer.setMouseTransparent(true);
        statsContainer.prefHeightProperty().bind(editorAreaPane.heightProperty());

        modelNodeTree = new ModelNodeTree(selectionNodeHandler, this);
        modelNodeTree.prefHeightProperty().bind(root.heightProperty());

        modelPropertyEditor = new ModelPropertyEditor(this);
        modelPropertyEditor.prefHeightProperty().bind(root.heightProperty());

        FXUtils.addToPane(statsContainer, editorAreaPane);
        FXUtils.addClassTo(statsContainer, CssClasses.SCENE_EDITOR_STATS_CONTAINER);
        FXUtils.addClassTo(modelNodeTree.getTreeView(), CssClasses.TRANSPARENT_TREE_VIEW);
    }

    @Override
    @FxThread
    protected void createToolComponents(@NotNull final EditorToolComponent container, @NotNull final StackPane root) {
        super.createToolComponents(container, root);

        container.addComponent(buildSplitComponent(getModelNodeTreeObjectsContainer(), getPropertyEditorObjectsContainer(), root),
                Messages.SCENE_FILE_EDITOR_TOOL_OBJECTS);
        container.addComponent(buildSplitComponent(getModelNodeTreeEditingContainer(), getPaintingComponentContainer(), root),
                Messages.SCENE_FILE_EDITOR_TOOL_PAINTING);
        container.addComponent(getScriptingComponent(), Messages.SCENE_FILE_EDITOR_TOOL_SCRIPTING);
    }

    /**
     * Refresh tree.
     */
    @FxThread
    protected void refreshTree() {

        final M currentModel = getCurrentModel();

        final ModelNodeTree modelNodeTree = getModelNodeTree();
        modelNodeTree.fill(currentModel);
    }

    /**
     * Process change tool.
     *
     * @param oldValue the old value
     * @param newValue the new value
     */
    @FxThread
    protected void processChangeTool(@Nullable final Number oldValue, @NotNull final Number newValue) {

        final ModelNodeTree modelNodeTree = getModelNodeTree();
        final ModelPropertyEditor modelPropertyEditor = getModelPropertyEditor();
        final PaintingComponentContainer editingComponentContainer = getPaintingComponentContainer();

        final VBox propertyEditorParent = (VBox) modelPropertyEditor.getParent();
        final VBox modelNodeTreeParent = (VBox) modelNodeTree.getParent();

        if (propertyEditorParent != null) {
            FXUtils.removeFromParent(modelPropertyEditor, propertyEditorParent);
        }

        if (modelNodeTreeParent != null) {
            FXUtils.removeFromParent(modelNodeTree, modelNodeTreeParent);
        }

        final int oldIndex = oldValue == null ? -1 : oldValue.intValue();
        final int newIndex = newValue.intValue();

        final VBox propertyContainer = getPropertyEditorObjectsContainer();

        if (newIndex == OBJECTS_TOOL) {
            FXUtils.addToPane(modelPropertyEditor, propertyContainer);
            FXUtils.addToPane(modelNodeTree, getModelNodeTreeObjectsContainer());
            modelPropertyEditor.rebuild();
        } else if (newIndex == PAINTING_TOOL) {
            FXUtils.addToPane(modelNodeTree, getModelNodeTreeEditingContainer());
            editingComponentContainer.notifyShowed();
        }

        if (oldIndex == PAINTING_TOOL) {
            editingComponentContainer.notifyHided();
        }

        final MA editor3DState = getEditor3DPart();
        editor3DState.changePaintingMode(newIndex == PAINTING_TOOL);
    }

    @Override
    @FxThread
    protected void handleDragDroppedEvent(@NotNull final DragEvent dragEvent) {
        super.handleDragDroppedEvent(dragEvent);

        UiUtils.handleDroppedFile(dragEvent, FileExtensions.JME_OBJECT, this,
                dragEvent, AbstractSceneFileEditor::addNewModel);

        UiUtils.handleDroppedFile(dragEvent, FileExtensions.JME_MATERIAL, this,
                dragEvent, AbstractSceneFileEditor::applyMaterial);
    }

    @Override
    @FxThread
    protected void handleDragOverEvent(@NotNull final DragEvent dragEvent) {
        super.handleDragOverEvent(dragEvent);
        UiUtils.acceptIfHasFile(dragEvent, ACCEPTED_FILES);
    }

    /**
     * Apply a new material from an asset tree.
     *
     * @param dragEvent the drag event.
     * @param file      the file.
     */
    @FxThread
    private void applyMaterial(@NotNull final DragEvent dragEvent, @NotNull final Path file) {

        final Path assetFile = notNull(getAssetFile(file), "Not found asset file for " + file);
        final String assetPath = toAssetPath(assetFile);

        final MA editor3DState = getEditor3DPart();
        final MaterialKey materialKey = new MaterialKey(assetPath);
        final Camera camera = editor3DState.getCamera();

        final BorderPane area = get3DArea();
        final Point2D areaPoint = area.sceneToLocal(dragEvent.getSceneX(), dragEvent.getSceneY());

        EXECUTOR_MANAGER.addJmeTask(() -> {

            final Geometry geometry = editor3DState.getGeometryByScreenPos((float) areaPoint.getX(),
                    camera.getHeight() - (float) areaPoint.getY());

            if (geometry == null) return;
            final Object linkNode = findParent(geometry, AssetLinkNode.class::isInstance);
            if (linkNode != null) return;

            final AssetManager assetManager = EditorUtil.getAssetManager();
            final Material material = assetManager.loadAsset(materialKey);

            final PropertyOperation<ChangeConsumer, Geometry, Material> operation =
                    new PropertyOperation<>(geometry, Messages.MODEL_PROPERTY_MATERIAL, material, geometry.getMaterial());

            operation.setApplyHandler(Geometry::setMaterial);

            execute(operation);
        });
    }

    /**
     * Add a new model from an asset tree.
     *
     * @param dragEvent the drag event.
     * @param file      the file.
     */
    @FxThread
    private void addNewModel(@NotNull final DragEvent dragEvent, @NotNull final Path file) {

        final M currentModel = getCurrentModel();
        if (!(currentModel instanceof Node)) {
            return;
        }

        final ModelNodeTree modelNodeTree = getModelNodeTree();
        final Object selected = modelNodeTree.getSelectedObject();

        final Node parent;

        if (selected instanceof Node && findParent((Spatial) selected, AssetLinkNode.class::isInstance) == null) {
            parent = (Node) selected;
        } else {
            parent = (Node) currentModel;
        }

        final Path assetFile = notNull(getAssetFile(file), "Not found asset file for " + file);
        final String assetPath = toAssetPath(assetFile);

        final MA editor3DState = getEditor3DPart();
        final ModelKey modelKey = new ModelKey(assetPath);
        final Camera camera = editor3DState.getCamera();

        final BorderPane area = get3DArea();
        final Point2D areaPoint = area.sceneToLocal(dragEvent.getSceneX(), dragEvent.getSceneY());

        EXECUTOR_MANAGER.addJmeTask(() -> {

            final SceneLayer defaultLayer = getDefaultLayer(this);
            final LocalObjects local = LocalObjects.get();

            final AssetManager assetManager = EditorUtil.getAssetManager();
            final Spatial loadedModel = assetManager.loadModel(modelKey);

            final AssetLinkNode assetLinkNode = new AssetLinkNode(modelKey);
            assetLinkNode.attachLinkedChild(loadedModel, modelKey);
            assetLinkNode.setUserData(LOADED_MODEL_KEY, true);

            if (defaultLayer != null) {
                SceneLayer.setLayer(defaultLayer, assetLinkNode);
            }

            final Vector3f scenePoint = editor3DState.getScenePosByScreenPos((float) areaPoint.getX(),
                    camera.getHeight() - (float) areaPoint.getY());
            final Vector3f result = local.nextVector(scenePoint)
                    .subtractLocal(parent.getWorldTranslation());

            assetLinkNode.setLocalTranslation(result);

            execute(new AddChildOperation(assetLinkNode, parent, false));
        });
    }


    /**
     * Handle changing select visibility.
     */
    @FxThread
    private void changeSelectionVisible(@NotNull final Boolean newValue) {
        if (isIgnoreListeners()) {
            return;
        }

        final MA editor3DPart = getEditor3DPart();
        editor3DPart.updateShowSelection(newValue);

        final ES editorState = getEditorState();
        if (editorState != null) {
            editorState.setEnableSelection(newValue);
        }
    }

    /**
     * Handle changing grid visibility.
     */
    @FxThread
    private void changeGridVisible(@NotNull final Boolean newValue) {
        if (isIgnoreListeners()) {
            return;
        }

        final MA editor3DPart = getEditor3DPart();
        editor3DPart.updateShowGrid(newValue);

        final ES editorState = getEditorState();
        if (editorState != null) {
            editorState.setEnableGrid(newValue);
        }
    }

    /**
     * Handle changing statistics visibility.
     */
    @FxThread
    private void changeStatisticsVisible(@NotNull final Boolean newValue) {
        if (isIgnoreListeners()) {
            return;
        }

        stats3DPart.setEnabled(newValue);

        final ES editorState = getEditorState();
        if (editorState != null) {
            editorState.setShowStatistics(newValue);
        }
    }

    /**
     * Notify about transformed the object.
     *
     * @param spatial the spatial
     */
    @FromAnyThread
    public void notifyTransformed(@NotNull final Spatial spatial) {
        EXECUTOR_MANAGER.addFxTask(() -> notifyTransformedImpl(spatial));
    }

    /**
     * Notify about transformed the object.
     */
    @FxThread
    private void notifyTransformedImpl(@NotNull final Spatial spatial) {

        Object toUpdate = spatial;

        if (spatial instanceof WrapperNode) {
            toUpdate = ((WrapperNode) spatial).getWrappedObject();
        }

        final ModelPropertyEditor propertyEditor = getModelPropertyEditor();
        propertyEditor.syncFor(toUpdate);
    }

    @Override
    @JmeThread
    public @NotNull Node getCursorNode() {
        return getEditor3DPart().getCursorNode();
    }

    @Override
    @JmeThread
    public  @NotNull Node getMarkersNode() {
        return getEditor3DPart().getMarkersNode();
    }
}


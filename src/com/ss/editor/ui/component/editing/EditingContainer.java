package com.ss.editor.ui.component.editing;

import com.ss.editor.model.undo.editor.ModelChangeConsumer;
import com.ss.editor.ui.css.CSSIds;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rlib.util.array.Array;
import rlib.util.array.ArrayFactory;

/**
 * The class container of editing components.
 *
 * @author JavaSaBr
 */
public class EditingContainer extends ScrollPane {

    /**
     * The change consumer.
     */
    @NotNull
    private final ModelChangeConsumer changeConsumer;

    /**
     * The list of editing components.
     */
    @NotNull
    private final Array<EditingComponent> components;

    /**
     * The container of editing component.
     */
    @NotNull
    private final VBox container;

    public EditingContainer(@NotNull final ModelChangeConsumer changeConsumer) {
        this.changeConsumer = changeConsumer;
        setId(CSSIds.EDITING_CONTAINER);
        this.components = ArrayFactory.newArray(EditingComponent.class);
        this.container = new VBox();
        setContent(container);
    }

    /**
     * Add a new editing component.
     *
     * @param editingComponent the editing component.
     */
    public void addComponent(@NotNull final EditingComponent editingComponent) {
        components.add(editingComponent);
    }

    /**
     * @return the container of editing component.
     */
    @NotNull
    private VBox getContainer() {
        return container;
    }

    /**
     * @return the list of editing components.
     */
    @NotNull
    private Array<EditingComponent> getComponents() {
        return components;
    }

    /**
     * Show an editing component to edit an element.
     *
     * @param element the element to edit.
     */
    public void showComponentFor(@Nullable final Object element) {

        final VBox container = getContainer();
        final ObservableList<Node> children = container.getChildren();
        children.clear();

        if (element == null) return;

        final EditingComponent editingComponent = getComponents().search(element, EditingComponent::isSupport);
        if (editingComponent == null) return;

        children.add((Node) editingComponent);
    }
}

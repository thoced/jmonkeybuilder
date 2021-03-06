package com.ss.editor.ui.control.model;

import com.ss.editor.model.undo.editor.ModelChangeConsumer;
import com.ss.editor.ui.control.tree.NodeTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The implementation of {@link NodeTree} to present a structure of model in an editor.
 *
 * @author JavaSaBr
 */
public class ModelNodeTree extends NodeTree<ModelChangeConsumer> {

    public ModelNodeTree(@NotNull final Consumer<Object> selectionHandler, @Nullable final ModelChangeConsumer consumer) {
        super(selectionHandler, consumer);
    }
}

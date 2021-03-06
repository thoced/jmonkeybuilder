package com.ss.editor.ui.component.asset.tree.context.menu.action;

import static com.ss.rlib.util.ClassUtils.unsafeCast;
import com.ss.editor.Messages;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.ui.Icons;
import com.ss.editor.ui.component.asset.tree.resource.ResourceElement;
import com.ss.editor.ui.event.impl.MovedFileEvent;
import com.ss.editor.ui.event.impl.RequestSelectFileEvent;
import com.ss.editor.util.EditorUtil;
import com.ss.rlib.util.FileUtils;
import com.ss.rlib.util.array.Array;
import com.ss.rlib.util.array.ArrayFactory;
import javafx.event.ActionEvent;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The action to paste a file.
 *
 * @author JavaSaBr
 */
public class PasteFileAction extends FileAction {

    public PasteFileAction(@NotNull final ResourceElement element) {
        super(element);
    }

    @FxThread
    @Override
    protected @NotNull String getName() {
        return Messages.ASSET_COMPONENT_RESOURCE_TREE_CONTEXT_MENU_PASTE_FILE;
    }

    @FxThread
    @Override
    protected @Nullable Image getIcon() {
        return Icons.PASTE_16;
    }

    @FxThread
    @Override
    protected void execute(@Nullable final ActionEvent event) {
        super.execute(event);

        final Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard == null) return;

        final List<File> files = unsafeCast(clipboard.getContent(DataFormat.FILES));
        if (files == null || files.isEmpty()) return;

        final Path currentFile = getElement().getFile();
        final boolean isCut = "cut".equals(clipboard.getContent(EditorUtil.JAVA_PARAM));

        if (isCut) {
            files.forEach(file -> moveFile(currentFile, file.toPath()));
        } else {
            files.forEach(file -> copyFile(currentFile, file.toPath()));
        }

        clipboard.clear();
    }

    private void copyFile(@NotNull final Path currentFile, @NotNull final Path file) {
        if (Files.isDirectory(currentFile)) {
            processCopy(currentFile, file);
        } else {
            processCopy(currentFile.getParent(), file);
        }
    }

    private void moveFile(@NotNull final Path currentFile, @NotNull final Path file) {

        if (Files.isDirectory(currentFile)) {
            processMove(currentFile, file);
        } else {
            processMove(currentFile.getParent(), file);
        }
    }

    /**
     * Process of moving.
     */
    private void processMove(@NotNull final Path targetFolder, @NotNull final Path file) {

        final Path newFile = targetFolder.resolve(file.getFileName());

        try {
            Files.move(file, newFile);
        } catch (final IOException e) {
            EditorUtil.handleException(LOGGER, this, e);
            return;
        }

        final MovedFileEvent event = new MovedFileEvent();
        event.setPrevFile(file);
        event.setNewFile(newFile);

        FX_EVENT_MANAGER.notify(event);
    }

    /**
     * Process of copying.
     */
    private void processCopy(@NotNull final Path targetFolder, @NotNull final Path file) {

        final Array<Path> toCopy = ArrayFactory.newArray(Path.class);
        final Array<Path> copied = ArrayFactory.newArray(Path.class);

        if (Files.isDirectory(file)) {
            toCopy.addAll(FileUtils.getFiles(file, true));
            toCopy.sort(FileUtils.FILE_PATH_LENGTH_COMPARATOR);
            toCopy.slowRemove(file);
        }

        final String freeName = FileUtils.getFirstFreeName(targetFolder, file);
        final Path newFile = targetFolder.resolve(freeName);

        try {
            processCopy(file, toCopy, copied, newFile);
        } catch (final IOException e) {
            EditorUtil.handleException(LOGGER, this, e);
        }

        final RequestSelectFileEvent event = new RequestSelectFileEvent();
        event.setFile(newFile);

        FX_EVENT_MANAGER.notify(event);
    }

    /**
     * Process of copying.
     */
    private void processCopy(@NotNull final Path file, @NotNull final Array<Path> toCopy,
                             @NotNull final Array<Path> copied, @NotNull final Path newFile) throws IOException {

        Files.copy(file, newFile);

        copied.add(newFile);
        toCopy.forEach(path -> {

            final Path relativeFile = file.relativize(path);
            final Path targetFile = newFile.resolve(relativeFile);

            try {
                Files.copy(path, targetFile);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            boolean needAddToCopied = true;

            for (final Path copiedFile : copied) {
                if (!Files.isDirectory(copiedFile)) continue;

                if (targetFile.startsWith(copiedFile)) {
                    needAddToCopied = false;
                    break;
                }
            }

            if (needAddToCopied) copied.add(targetFile);
        });
    }
}

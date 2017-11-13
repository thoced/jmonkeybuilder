package com.ss.editor.ui.control.property.impl;

import static com.ss.rlib.geom.util.AngleUtils.degreeToRadians;
import static com.ss.rlib.geom.util.AngleUtils.radiansToDegree;
import static com.ss.rlib.util.ObjectUtils.notNull;
import com.jme3.math.Quaternion;
import com.ss.editor.annotation.FXThread;
import com.ss.editor.model.undo.editor.ChangeConsumer;
import com.ss.editor.ui.control.property.PropertyControl;
import com.ss.editor.ui.css.CSSClasses;
import com.ss.editor.ui.util.UIUtils;
import com.ss.rlib.ui.control.input.FloatTextField;
import com.ss.rlib.ui.util.FXUtils;
import com.ss.rlib.util.array.ArrayFactory;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The implementation of the {@link PropertyControl} to edit {@link Quaternion} values.
 *
 * @param <C> the type parameter
 * @param <T> the type parameter
 * @author JavaSaBr
 */
public class QuaternionPropertyControl<C extends ChangeConsumer, T> extends PropertyControl<C, T, Quaternion> {

    /**
     * The field Y.
     */
    @Nullable
    private FloatTextField xField;

    /**
     * The field X.
     */
    @Nullable
    private FloatTextField yField;

    /**
     * The field Z.
     */
    @Nullable
    private FloatTextField zField;

    public QuaternionPropertyControl(@Nullable final Quaternion propertyValue, @NotNull final String propertyName,
                                     @NotNull final C changeConsumer) {
        super(propertyValue, propertyName, changeConsumer);
    }

    @Override
    @FXThread
    protected void createComponents(@NotNull final HBox container) {
        super.createComponents(container);

        final Label xLabel = new Label("x:");

        xField = new FloatTextField();
        xField.setOnKeyReleased(this::updateRotation);
        xField.addChangeListener((observable, oldValue, newValue) -> updateRotation(null));
        xField.prefWidthProperty().bind(widthProperty().divide(3));

        final Label yLabel = new Label("y:");

        yField = new FloatTextField();
        yField.setOnKeyReleased(this::updateRotation);
        yField.addChangeListener((observable, oldValue, newValue) -> updateRotation(null));
        yField.prefWidthProperty().bind(widthProperty().divide(3));

        final Label zLabel = new Label("z:");

        zField = new FloatTextField();
        zField.setOnKeyReleased(this::updateRotation);
        zField.addChangeListener((observable, oldValue, newValue) -> updateRotation(null));
        zField.prefWidthProperty().bind(widthProperty().divide(3));

        FXUtils.addToPane(xLabel, container);
        FXUtils.addToPane(xField, container);
        FXUtils.addToPane(yLabel, container);
        FXUtils.addToPane(yField, container);
        FXUtils.addToPane(zLabel, container);
        FXUtils.addToPane(zField, container);

        FXUtils.addClassTo(xLabel, yLabel, zLabel, CSSClasses.ABSTRACT_PARAM_CONTROL_NUMBER_LABEL);
        FXUtils.addClassesTo(container, CSSClasses.DEF_HBOX, CSSClasses.TEXT_INPUT_CONTAINER,
                CSSClasses.ABSTRACT_PARAM_CONTROL_INPUT_CONTAINER);
        FXUtils.addClassesTo(xField, yField, zField, CSSClasses.ABSTRACT_PARAM_CONTROL_VECTOR3F_FIELD,
                CSSClasses.TRANSPARENT_TEXT_FIELD);

        UIUtils.addFocusBinding(container, xField, yField, zField);
    }

    @Override
    @FXThread
    protected void setPropertyValue(@Nullable final Quaternion quaternion) {
        super.setPropertyValue(quaternion == null ? null : quaternion.clone());
    }

    /**
     * @return the field X.
     */
    @FXThread
    private @NotNull FloatTextField getXField() {
        return notNull(xField);
    }

    /**
     * @return the field Y.
     */
    @FXThread
    private @NotNull FloatTextField getYFiled() {
        return notNull(yField);
    }

    /**
     * @return the field Z.
     */
    @FXThread
    private @NotNull FloatTextField getZField() {
        return notNull(zField);
    }

    @Override
    @FXThread
    protected void reload() {

        final float[] angles = new float[3];

        final Quaternion element = getPropertyValue();
        notNull(element, "The property value can't be null.");

        element.toAngles(angles);

        final FloatTextField xField = getXField();
        xField.setValue(radiansToDegree(angles[0]));
        xField.positionCaret(xField.getText().length());

        final FloatTextField yFiled = getYFiled();
        yFiled.setValue(radiansToDegree(angles[1]));
        yFiled.positionCaret(yFiled.getText().length());

        final FloatTextField zField = getZField();
        zField.setValue(radiansToDegree(angles[2]));
        zField.positionCaret(zField.getText().length());
    }

    /**
     * Updating rotation.
     */
    @FXThread
    private void updateRotation(@Nullable final KeyEvent event) {
        if (isIgnoreListener() || (event != null && event.getCode() != KeyCode.ENTER)) return;

        final Quaternion oldValue = notNull(getPropertyValue());

        final FloatTextField xField = getXField();
        final float x = degreeToRadians(xField.getValue());

        final FloatTextField yFiled = getYFiled();
        final float y = degreeToRadians(yFiled.getValue());

        final FloatTextField zField = getZField();
        final float z = degreeToRadians(zField.getValue());

        final Quaternion newValue = new Quaternion();
        newValue.fromAngles(ArrayFactory.toFloatArray(x, y, z));

        changed(newValue, oldValue.clone());
    }
}
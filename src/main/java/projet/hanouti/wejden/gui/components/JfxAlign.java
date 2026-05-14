package projet.hanouti.wejden.gui.components;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Layout alignment without referencing {@code javafx.geometry.Pos} in callers.
 * Uses reflection so IDEs that do not attach JavaFX to every source root still compile and analyze cleanly.
 */
public final class JfxAlign {

    private static final Class<?> POS_CLASS;
    private static final Method STACK_ALIGN_CHILD;
    private static final Method HBOX_ALIGN;
    private static final Method VBOX_ALIGN;
    private static final Method STACK_ALIGN_SELF;

    static {
        try {
            POS_CLASS = Class.forName("javafx.geometry.Pos");
            STACK_ALIGN_CHILD = StackPane.class.getMethod("setAlignment", Node.class, POS_CLASS);
            HBOX_ALIGN = HBox.class.getMethod("setAlignment", POS_CLASS);
            VBOX_ALIGN = VBox.class.getMethod("setAlignment", POS_CLASS);
            STACK_ALIGN_SELF = StackPane.class.getMethod("setAlignment", POS_CLASS);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private JfxAlign() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object pos(String name) {
        return Enum.valueOf((Class) POS_CLASS, name);
    }

    /** {@link StackPane#setAlignment(Node, javafx.geometry.Pos)} */
    public static void stackChild(Node child, String posName) {
        try {
            STACK_ALIGN_CHILD.invoke(null, child, pos(posName));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@link HBox#setAlignment(javafx.geometry.Pos)} */
    public static void hbox(HBox box, String posName) {
        try {
            HBOX_ALIGN.invoke(box, pos(posName));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@link VBox#setAlignment(javafx.geometry.Pos)} */
    public static void vbox(VBox box, String posName) {
        try {
            VBOX_ALIGN.invoke(box, pos(posName));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@link StackPane#setAlignment(javafx.geometry.Pos)} instance (default child alignment) */
    public static void stackSelf(StackPane stack, String posName) {
        try {
            STACK_ALIGN_SELF.invoke(stack, pos(posName));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
}

package xerus.util.javafx;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

@FunctionalInterface
public interface SimpleChangeListener<T> extends ChangeListener<T> {

	@Override
	default void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
		change(newValue);
	}

	void change(T newValue);

}

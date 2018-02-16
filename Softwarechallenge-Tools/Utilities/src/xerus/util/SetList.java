package xerus.util;
import java.util.ArrayList;
import java.util.Collection;

/** Implementation of ArrayList which prohibits double Elements */
public class SetList<E> extends ArrayList<E> {

	public SetList() {
		super();
	}

	public SetList(Collection<E> list) {
		super(list);
	}

	@Override
	public boolean add(E e) {
		return contains(e) ? false : super.add(e);
	}

	@Override
	public void add(int index, E e) {
		if (!contains(e)) {
			super.add(index, e);
		}
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return addAll(size(), c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		Collection<E> copy = new ArrayList<E>(c);
		copy.removeAll(this);
		return super.addAll(index, copy);
	}

}
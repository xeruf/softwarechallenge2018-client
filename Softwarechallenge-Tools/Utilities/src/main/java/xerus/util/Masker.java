package xerus.util;

public class Masker {
	
	private String[] toMask;
	private String mask;
	
	public Masker(String mask, String... toMask) {
		this.mask = mask;
		this.toMask = toMask;
	}
	
	public String mask(String s) {
		for (int i = 0; i < toMask.length; i++)
			s = s.replace(toMask[i], mask + i);
		return s;
	}
	
	public String unmask(String s) {
		for (int i = 0; i < toMask.length; i++)
			s = s.replace(mask + i, toMask[i]);
		return s;
	}

}

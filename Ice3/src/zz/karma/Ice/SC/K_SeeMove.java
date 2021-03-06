package zz.karma.Ice.SC;

import azura.karma.run.Karma;
import azura.karma.run.KarmaReaderA;
import azura.karma.def.KarmaSpace;

/**
*@note empty
*/
public class K_SeeMove extends KarmaReaderA {
	public static final int type = 113916805;

	public K_SeeMove(KarmaSpace space) {
		super(space, type , 113917438);
	}

	@Override
	public void fromKarma(Karma karma) {
		if(karma==null) return;
		id = karma.getInt(0);
		x = karma.getInt(1);
		y = karma.getInt(2);
		angle = karma.getInt(3);
	}

	@Override
	public Karma toKarma() {
		karma.setInt(0, id);
		karma.setInt(1, x);
		karma.setInt(2, y);
		karma.setInt(3, angle);
		return karma;
	}

	/**
	*@type INT
	*@note empty
	*/
	public int id;
	/**
	*@type INT
	*@note empty
	*/
	public int x;
	/**
	*@type INT
	*@note empty
	*/
	public int y;
	/**
	*@type INT
	*@note empty
	*/
	public int angle;

}
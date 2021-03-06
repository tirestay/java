package azura.banshee.zebra2.tif2;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;

import com.google.common.math.IntMath;

import azura.banshee.chessboard.fi.PyramidFi;
import azura.banshee.util.StreamImageReader;
import azura.banshee.util.TextureType;
import azura.banshee.util.TiffWriter;
import azura.gallerid.Swap;
import common.algorithm.FastMath;
import common.algorithm.FoldIndex;
import common.collections.RectanglePlus;
import common.graphics.ImageUtil;

public class ImageReader2 extends PyramidFi<ImageReaderTile2> {
	static {
		System.setProperty("com.sun.media.jai.disableMediaLib", "true");
	}
	private File tempFolder = Swap.applyNewSwapSubFolder();

	StreamImageReader[] sourceArray;
	BufferedImage rootImage;
	/**
	 * estimated value. should wrap around accurate value
	 */
	public RectanglePlus bbc = new RectanglePlus();
	// public int sourceWidth, sourceHeight;
	private int width, height;
	Color bgColor;// = new Color(0x0, false);
	public TextureType rootType;

	public ImageReader2(StreamImageReader original, Color fillColor, boolean smooth) {
		this.bgColor = fillColor;

		int sourceWidth = original.image.getWidth();
		int sourceHeight = original.image.getHeight();

		int side = Math.max(sourceWidth, sourceHeight);
		int side2n = FastMath.getNextPowerOfTwo(side);
		super.zMax = FastMath.log2(side2n / tileSide);

		loadSourceArray(original, smooth);

		rootImage = sourceArray[0].image.getAsBufferedImage();
		rootType = TextureType.check(rootImage);

		bbc = ImageUtil.getBoundingBox(rootImage);
		bbc = new RectanglePlus(bbc.x - 1, bbc.y - 1, bbc.width + 2, bbc.height + 2);
		bbc.x <<= zMax;
		bbc.y <<= zMax;
		bbc.width <<= zMax;
		bbc.height <<= zMax;

		// cannot exceed original
		if (bbc.getLeft() < 0)
			bbc.setLeft(0);
		if (bbc.getCeiling() < 0)
			bbc.setCeiling(0);
		if (bbc.getRight() >= sourceWidth)
			bbc.setRight(sourceWidth - 1);
		if (bbc.getFloor() >= sourceHeight)
			bbc.setFloor(sourceHeight - 1);

		// to center coordinate
		bbc.x -= sourceWidth / 2;
		bbc.y -= sourceHeight / 2;

		width = Math.max(Math.abs(bbc.x), Math.abs(bbc.getRight() + 1)) * 2;
		height = Math.max(Math.abs(bbc.y), Math.abs(bbc.getFloor() + 1)) * 2;

		int sideB = Math.max(width, height);
		int side2nB = FastMath.getNextPowerOfTwo(sideB);
		int zMaxB = FastMath.log2(side2nB / tileSide);
		if (zMaxB != zMax) {
			int delta = zMax - zMaxB;
			StreamImageReader[] s2 = new StreamImageReader[zMaxB + 1];
			for (int i = 0; i <= zMaxB; i++) {
				s2[i] = sourceArray[i + delta];
			}
			for (int i = 0; i < delta; i++) {
				sourceArray[i].dispose();
			}
			super.zMax = zMaxB;
			sourceArray = s2;
			rootImage = sourceArray[0].image.getAsBufferedImage();
		}

		RectanglePlus bbcRoot = bbc.clone();
		bbcRoot.setLeft(bbc.getLeft() >> zMax);
		bbcRoot.setRight(bbc.getRight() >> zMax);
		bbcRoot.setCeiling(bbc.getCeiling() >> zMax);
		bbcRoot.setFloor(bbc.getFloor() >> zMax);

		// ================== ToDo: root looks different from others?
		// ================
		BufferedImage temp = ImageUtil.newImage(tileSide, tileSide, bgColor);
		for (int i = bbcRoot.getLeft(); i <= bbcRoot.getRight(); i++)
			for (int j = bbcRoot.getCeiling(); j <= bbcRoot.getFloor(); j++) {
				int x = i + rootImage.getWidth() / 2;
				int y = j + rootImage.getHeight() / 2;
				if (x < 0 || x >= rootImage.getWidth() || y < 0 || y >= rootImage.getHeight())
					continue;

				int c = rootImage.getRGB(x, y);
				temp.setRGB(i + tileSide / 2, j + tileSide / 2, c);
			}
		rootImage = temp;
	}

	private void loadSourceArray(StreamImageReader source, boolean smooth) {
		this.sourceArray = new StreamImageReader[zMax + 1];

		sourceArray[zMax] = source;
		for (int level = zMax - 1; level >= 0; level--) {
			String cacheName = getCacheName(level);
			sourceArray[level] = resizeAndStore(sourceArray[level + 1], 0.5f, cacheName, smooth);
		}
	}

	public int getRGB(int x, int y, int z) {
		if (!bbc.contains(x, y))
			throw new Error();

		if (z > zMax || z < 0) {
			throw new Error("invalid z");
		} else if (z == 0) {
			if (-tileSide / 2 <= x && x < tileSide / 2 && -tileSide / 2 <= y && y < tileSide / 2) {
				return rootImage.getRGB(x + tileSide / 2, y + tileSide / 2);
			} else {
				log.info("out of bound:  (" + x + "," + y + ",0)");
				return 0;
			}
		} else {
			ImageReaderTile2 tile = getTile(x, y, z);
			if (tile == null) {
				log.info("out of bound:  (" + x + "," + y + "," + z + ")");
				return 0;
			}

			x = IntMath.mod(x, tileSide);
			y = IntMath.mod(y, tileSide);
			return tile.getRGB(x, y);
		}
	}

	public static ImageReader2 load(String fileName, Color fillColor, boolean smooth) {
		StreamImageReader source = new StreamImageReader(fileName);
		return new ImageReader2(source, fillColor, smooth);
	}

	public void dispose() {
		for (FoldIndex fi : FoldIndex.getAllFiInPyramid(zMax)) {
			ImageReaderTile2 tt = getTile(fi.fi);
			tt.dispose();
		}
		for (StreamImageReader tiff : sourceArray) {
			tiff.dispose();
		}
	}

	@Override
	protected ImageReaderTile2 createTile(int fi) {
		return new ImageReaderTile2(fi, this);
	}

	private String getCacheName(int level) {
		return tempFolder.getPath() + "/" + level + ".tif";
	}

	public int frameWidth(int z) {
		return (width >> (zMax - z)) + 1;
	}

	public int frameHeight(int z) {
		return (height >> (zMax - z)) + 1;
	}

	private StreamImageReader resizeAndStore(StreamImageReader source, float scale, String fileName, boolean smooth) {

		PlanarImage scaledImage = StreamImageReader.scale(source.image, scale, smooth);
		TiffWriter.write(scaledImage, fileName, tileSide);
		new File(fileName).deleteOnExit();
		return new StreamImageReader(fileName);
	}

	public static void main(String[] args) throws IOException {
		args = new String[] { "Z:/temp/zebra/pole.png" };

		File input = new File(args[0]);

		ImageReader2 reader = load(input.getPath(), Color.red, false);
		for (int zUp = 0; zUp <= reader.zMax; zUp++) {
			int w = (reader.width >> zUp) + 1;
			int h = (reader.height >> zUp) + 1;
			BufferedImage layer = ImageUtil.newImage(w, h, Color.BLACK);
			int left = reader.bbc.getLeft() >> zUp;
			int top = reader.bbc.getCeiling() >> zUp;
			int right = reader.bbc.getRight() >> zUp;
			int bottom = reader.bbc.getFloor() >> zUp;
			for (int i = left; i <= right; i++)
				for (int j = top; j <= bottom; j++) {
					int color = reader.getRGB(i, j, reader.zMax - zUp);
					int lx = i + layer.getWidth() / 2;
					int ly = j + layer.getHeight() / 2;
					layer.setRGB(lx, ly, color);
				}

			ImageIO.write(layer, "png", new File(input.getPath() + "." + (reader.zMax - zUp) + ".tif"));
		}
		reader.dispose();

		System.out.println(reader.bbc);
	}
}

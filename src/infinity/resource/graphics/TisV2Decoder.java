// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles new PVRZ-based TIS resources.
 * @author argent77
 */
public class TisV2Decoder extends TisDecoder
{
  private static final int HeaderSize = 24;   // Size of the TIS header
  private static final Color TransparentColor = new Color(0, true);

  private final ConcurrentHashMap<Integer, PvrDecoder> pvrTable = new ConcurrentHashMap<Integer, PvrDecoder>();

  private byte[] tisData;
  private int tileCount, tileSize;
  private String pvrzNameBase;
  private BufferedImage workingCanvas;

  public TisV2Decoder(ResourceEntry tisEntry)
  {
    super(tisEntry);
    init();
  }

  /**
   * Returns the base filename of the PVRZ resource (without page suffix and extension).
   * @return Base filename of the PVRZ resource related to this TIS resource.
   */
  public String getPvrzFileBase()
  {
    return pvrzNameBase;
  }

  /**
   * Returns the page index of the PVRZ resource containing the graphics data of the specified tile.
   * @param tileIdx The tile index.
   * @return A page index that can be used to determine the PVRZ resource that contains the graphics
   *         data of the specified tile. Returns -1 on error.
   */
  public int getPvrzPage(int tileIdx)
  {
    int ofs = getTileOffset(tileIdx);
    if (ofs > 0) {
      return DynamicArray.getInt(tisData, ofs);
    } else {
      return -1;
    }
  }

  /**
   * Returns the full PVRZ resource filename that contains the graphics data of the specified tile.
   * @param tileIdx The tile index
   * @return Full PVRZ resource filename with page and extension. Returns an empty string on error.
   */
  public String getPvrzFileName(int tileIdx)
  {
    int page = getPvrzPage(tileIdx);
    if (page >= 0) {
      return String.format("%1$s%2$02d.PVRZ", getPvrzFileBase(), page);
    } else {
      return "";
    }
  }

  /**
   * Forces the internal cache to be filled with all PVRZ resources required by this TIS resource.
   * This will ensure that tiles are decoded at constant speed.
   */
  public void preloadPvrz()
  {
    for (int i = 0; i < getTileCount(); i++) {
      int page = getPvrzPage(i);
      if (page >= 0) {
        getPVR(page);
      }
    }
  }

  /**
   * This is the counterpart of {@link #preloadPvrz()}. It removes all cached instances of
   * PVRZ resources.
   */
  public void flush()
  {
    // properly removing PvrDecoder objects
    Iterator<Integer> iter = pvrTable.keySet().iterator();
    while (iter.hasNext()) {
      PvrDecoder d = pvrTable.get(iter.next());
      if (d != null) {
        d.close();
        d = null;
      }
    }
    pvrTable.clear();
  }


  @Override
  public void close()
  {
    flush();

    tisData = null;
    tileCount = 0;
    tileSize = 0;
    pvrzNameBase = "";
    if (workingCanvas != null) {
      workingCanvas.flush();
      workingCanvas = null;
    }
  }

  @Override
  public void reload()
  {
    init();
  }

  @Override
  public byte[] getResourceData()
  {
    return tisData;
  }

  @Override
  public int getTileWidth()
  {
    return TileDimension;
  }

  @Override
  public int getTileHeight()
  {
    return TileDimension;
  }

  @Override
  public int getTileCount()
  {
    return tileCount;
  }

  @Override
  public Image getTile(int tileIdx)
  {
    BufferedImage image = ColorConvert.createCompatibleImage(TileDimension, TileDimension, true);
    renderTile(tileIdx, image);
    return image;
  }

  @Override
  public boolean getTile(int tileIdx, Image canvas)
  {
    return renderTile(tileIdx, canvas);
  }

  @Override
  public int[] getTileData(int tileIdx)
  {
    int[] buffer = new int[TileDimension*TileDimension];
    renderTile(tileIdx, buffer);
    return buffer;
  }

  @Override
  public boolean getTileData(int tileIdx, int[] buffer)
  {
    return renderTile(tileIdx, buffer);
  }


  private void init()
  {
    close();
    if (getResourceEntry() != null) {
      try {
        int[] info = getResourceEntry().getResourceInfo();
        if (info == null || info.length < 2) {
          throw new Exception("Error reading TIS header");
        }

        tileCount = info[0];
        if (tileCount <= 0) {
          throw new Exception("Invalid tile count: " + tileCount);
        }
        tileSize = info[1];
        if (tileSize != 12) {
          throw new Exception("Invalid tile size: " + tileSize);
        }
        tisData = getResourceEntry().getResourceData();

        String name = getResourceEntry().getResourceName();
        int idx = name.lastIndexOf('.');
        if (idx < 0) idx = name.length();
        pvrzNameBase = getResourceEntry().getResourceName().substring(0, 1) +
                       getResourceEntry().getResourceName().substring(2, idx);

        setType(Type.PVRZ);

        workingCanvas = new BufferedImage(TileDimension, TileDimension, BufferedImage.TYPE_INT_ARGB);
      } catch (Exception e) {
        e.printStackTrace();
        close();
      }
    }
  }

  // Returns and caches the PVRZ resource of the specified page
  private PvrDecoder getPVR(int page)
  {
    synchronized (pvrTable) {
      Integer key = Integer.valueOf(page);
      if (pvrTable.containsKey(key)) {
        return pvrTable.get(key);
      }

      try {
        String name = String.format("%1$s%2$02d.PVRZ", pvrzNameBase, page);
        ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(name);
        if (entry != null) {
          byte[] data = entry.getResourceData();
          if (data != null) {
            int size = DynamicArray.getInt(data, 0);
            int marker = DynamicArray.getUnsignedShort(data, 4);
            if ((size & 0xff) == 0x34 && marker == 0x9c78) {
              data = Compressor.decompress(data, 0);
              PvrDecoder decoder = new PvrDecoder(data);
              pvrTable.put(key, decoder);
              return decoder;
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  // Returns the start offset of the specified tile. Returns -1 on error.
  private int getTileOffset(int tileIdx)
  {
    if (tileIdx >= 0 && tileIdx < getTileCount()) {
      return HeaderSize + tileIdx*tileSize;
    } else {
      return -1;
    }
  }

  // Paints the specified tile onto the canvas
  private boolean renderTile(int tileIdx, Image canvas)
  {
    if (canvas != null && canvas.getWidth(null) >= TileDimension && canvas.getHeight(null) >= TileDimension) {
      if (updateWorkingCanvas(tileIdx)) {
        Graphics2D g = (Graphics2D)canvas.getGraphics();
        try {
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
          g.setColor(TransparentColor);
          g.fillRect(0, 0, TileDimension, TileDimension);
          g.drawImage(workingCanvas, 0, 0, null);
        } finally {
          g.dispose();
          g = null;
        }
        return true;
      }
    }
    return false;
  }

  // Writes the specified tile data into the buffer
  private boolean renderTile(int tileIdx, int[] buffer)
  {
    int size = TileDimension*TileDimension;
    if (buffer != null && buffer.length >= size) {
      if (updateWorkingCanvas(tileIdx)) {
        int[] src = ((DataBufferInt)workingCanvas.getRaster().getDataBuffer()).getData();
        System.arraycopy(src, 0, buffer, 0, size);
        src = null;
        return true;
      }
    }
    return false;
  }

  // Draws the specified tile into the working canvas and returns the success state.
  private boolean updateWorkingCanvas(int tileIdx)
  {
    int ofs = getTileOffset(tileIdx);
    if (ofs > 0) {
      int page = DynamicArray.getInt(tisData, ofs);
      int x = DynamicArray.getInt(tisData, ofs+4);
      int y = DynamicArray.getInt(tisData, ofs+8);
            PvrDecoder decoder = getPVR(page);
      if (decoder != null || page == -1) {
        // removing old content
        try {
          Graphics2D g = (Graphics2D)workingCanvas.getGraphics();
          try {
            if (page == -1) {
              g.setColor(Color.BLACK);
            } else {
              g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
              g.setColor(TransparentColor);
            }
            g.fillRect(0, 0, TileDimension, TileDimension);
          } finally {
            g.dispose();
            g = null;
          }

          if (decoder != null) {
            // drawing new content
            decoder.decode(workingCanvas, x, y, TileDimension, TileDimension);
            decoder = null;
          }
          return true;
        } catch (Exception e) {
          e.printStackTrace();
          decoder = null;
        }
      }
    }
    return false;
  }
}
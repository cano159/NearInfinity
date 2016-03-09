// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.io.FileReaderNI;

/**
 * Common base class for handling BAM resources.
 * @author argent77
 */
public abstract class BamDecoder
{
  /** Recognized BAM resource types */
  public enum Type { INVALID, BAMC, BAMV1, BAMV2, CUSTOM }

  private final ResourceEntry bamEntry;

  private Type type;

  /**
   * Returns whether the specified resource entry points to a valid BAM resource.
   */
  public static boolean isValid(ResourceEntry bamEntry)
  {
    return getType(bamEntry) != Type.INVALID;
  }

  /**
   * Returns the type of the specified resource entry.
   * @return One of the BAM <code>Type</code>s.
   */
  public static Type getType(ResourceEntry bamEntry)
  {
    Type retVal = Type.INVALID;
    if (bamEntry != null) {
      try {
        InputStream is = bamEntry.getResourceDataAsStream();
        if (is != null) {
          String signature = FileReaderNI.readString(is, 4);
          String version = FileReaderNI.readString(is, 4);
          is.close();
          if ("BAMC".equals(signature)) {
            retVal = Type.BAMC;
          } else if ("BAM ".equals(signature)) {
            if ("V1  ".equals(version)) {
              retVal = Type.BAMV1;
            } else if ("V2  ".equals(version)) {
              retVal = Type.BAMV2;
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return retVal;
  }

  /**
   * Returns a new BamDecoder object based on the specified Bam resource entry.
   * @param bamEntry The BAM resource entry.
   * @return Either <code>BamV1Decoder</code> or <code>BamV2Decoder</code>, depending on the
   *         BAM resource type. Returns <code>null</code> if the resource doesn't contain valid
   *         BAM data.
   */
  public static BamDecoder loadBam(ResourceEntry bamEntry)
  {
    Type type = getType(bamEntry);
    switch (type) {
      case BAMC:
      case BAMV1:
        return new BamV1Decoder(bamEntry);
      case BAMV2:
        return new BamV2Decoder(bamEntry);
      default:
        return null;
    }
  }

  /**
   * Returns a new BamDecoder object of type PseudoBamDecoder.
   * @param image The input image to create a pseudo BAM of one frame.
   * @return A PseudoBamDecoder object.
   */
  public static BamDecoder loadBam(BufferedImage image)
  {
    return loadBam(new BufferedImage[]{image});
  }

  /**
   * Returns a new BamDecoder object of type PseudoBamDecoder.
   * @param images An array of input images to create the pseudo BAM structure.
   * @return A PseudoBamDecoder object.
   */
  public static BamDecoder loadBam(BufferedImage[] images)
  {
    return new PseudoBamDecoder(images);
  }


  /**
   * Returns the ResourceEntry object of the BAM resource.
   */
  public ResourceEntry getResourceEntry()
  {
    return bamEntry;
  }

  /**
   * Returns <code>true</code> if the BAM has been closed, is invalid or does not contain any frames.
   */
  public boolean isEmpty()
  {
    return frameCount() == 0;
  }

  /** Returns the type of the BAM resource. */
  public Type getType()
  {
    return type;
  }

  /** Returns an interface that provides access to cycle-specific functionality.  */
  public abstract BamControl createControl();

  /** Returns a frame info object containing basic properties of the specified frame. */
  public abstract FrameEntry getFrameInfo(int frameIdx);

  /** Removes all data from the decoder. Use this to free up memory. */
  public abstract void close();
  /** Returns whether the BamDecoder instance has been initialized correctly. */
  public abstract boolean isOpen();
  /** Clears existing data and reloads the current BAM resource entry. */
  public abstract void reload();

  /** Returns the raw data of the BAM resource. */
  public abstract byte[] getResourceData();

  /** Returns the total number of available frames. */
  public abstract int frameCount();

  /** Returns the specified frame as Image. */
  public abstract Image frameGet(BamControl control, int frameIdx);
  /** Draws the specified frame onto the canvas. */
  public abstract void frameGet(BamControl control, int frameIdx, Image canvas);


  protected BamDecoder(ResourceEntry bamEntry)
  {
    this.bamEntry = bamEntry;
    this.type = Type.INVALID;
  }


  // Sets the current BAM type
  protected void setType(Type type)
  {
    this.type = type;
  }


//-------------------------- INNER CLASSES --------------------------

  /**
   * Interface for basic BAM frame properties
   */
  public interface FrameEntry
  {
    public int getWidth();
    public int getHeight();
    public int getCenterX();
    public int getCenterY();
  }


  /**
   * Provides access to cycle-specific functionality.
   */
  public static abstract class BamControl
  {
    /**
     * Definitions of how to render BAM frames.<br>
     * <b>Individual:</b> Each frame is drawn individually. The resulting image dimension is defined
     *                    by the drawn frame. Does not take frame centers into account.<br>
     * <b>Shared:</b> Each frame is drawn onto a canvas of fixed dimension that is big enough to hold
     *                every single frame without cropping or resizing. Takes frame centers into account.
     */
    public enum Mode { INDIVIDUAL, SHARED }

    private final BamDecoder parent;

    private Mode mode;
    // Contains the image dimension of all BAMs used in Shared mode.
    // Dimension(width, height) defines the total image dimension.
    // Point(x, y) defines the position for center (0, 0).
    private Rectangle sharedBamSize;
    private boolean sharedPerCycle;

    protected BamControl(BamDecoder parent)
    {
      this.parent = parent;
      this.mode = Mode.INDIVIDUAL;
      this.sharedBamSize = new Rectangle();
      this.sharedPerCycle = true;
    }

    /**
     * Returns the currently selected drawing mode.
     */
    public Mode getMode()
    {
      return mode;
    }

    /**
     * Specify how to draw graphical frame data. It affects all methods that draw a frame into an
     * Image object or an int array.
     * @param mode The drawing mode.
     */
    public void setMode(Mode mode)
    {
      if (mode != null && mode != this.mode) {
        this.mode = mode;
        updateSharedBamSize();
      }
    }

    /**
     * Returns whether the calculated image dimension in shared mode is based on the current cycle.
     */
    public boolean isSharedPerCycle()
    {
      return sharedPerCycle;
    }

    /**
     * Sets whether the calculated image dimension in shared mode is based on the current cycle.
     */
    public void setSharedPerCycle(boolean set)
    {
      if (set != sharedPerCycle) {
        sharedPerCycle = set;
        updateSharedBamSize();
      }
    }

    /**
     * Calculates the rectangle of the current BAM animation. Takes {@link #isSharedPerCycle()} into account.
     * @param isMirrored If <code>true</code>, returns a rectangle that is based on the
     *                   animation mirrored along the x axis.
     * @return A rectangle containing information about the base offset (x, y) and
     *         overall dimension (width, height).
     */
    public Rectangle calculateSharedCanvas(boolean isMirrored)
    {
      return calculateSharedBamSize(null, isSharedPerCycle(), isMirrored);
    }

    /**
     * Returns the dimension of the image for frames to be drawn in shared mode.
     */
    public Dimension getSharedDimension()
    {
      if (sharedBamSize != null) {
        return new Dimension(sharedBamSize.getSize());
      } else {
        return new Dimension(1, 1);
      }
    }

    /**
     * Returns the point of origin for frames that are drawn in shared mode.<br>
     * The top-left corner of the selected frame is calculated as:<br>
     * <code>topleft = getSharedOrigin() - frameCenter()</code>
     */
    public Point getSharedOrigin()
    {
      if (sharedBamSize != null) {
        return new Point(sharedBamSize.getLocation());
      } else {
        return new Point();
      }
    }


    /** Returns the BamDecoder instance this control is associated with. */
    public BamDecoder getDecoder()
    {
      return parent;
    }

    /** Returns <code>true</code> if, and only if {@link #cycleCount()} is 0. */
    public boolean isEmpty()
    {
      return (cycleCount() == 0);
    }

    /** Returns the total number of available cycles. */
    public abstract int cycleCount();

    /** Returns the number of frames in the currently selected cycle. */
    public abstract int cycleFrameCount();
    /** Returns the number of frames in the specified cycle. */
    public abstract int cycleFrameCount(int cycleIdx);

    /** Returns the index of the active cycle. */
    public abstract int cycleGet();
    /** Sets the active cycle. (Default: first available cycle) */
    public abstract boolean cycleSet(int cycleIdx);

    /** Returns whether the active cycle can be advanced by at least one more frame. */
    public abstract boolean cycleHasNextFrame();
    /** Selects the next available frame in the active cycle if available. Returns whether a next frame has been selected. */
    public abstract boolean cycleNextFrame();
    /** Selects the first frame in the active cycle. */
    public abstract void cycleReset();

    /** Returns the currently selected frame of the active cycle as Image. (Takes current mode into account.) */
    public abstract Image cycleGetFrame();
    /** Draws the currently selected frame of the active cycle onto the specified canvas. (Takes current mode into account.) */
    public abstract void cycleGetFrame(Image canvas);
    /** Returns the specified frame of the active cycle as Image. (Takes current mode into account.) */
    public abstract Image cycleGetFrame(int frameIdx);
    /** Draws the specified frame of the active cycle onto the specified canvas. (Takes current mode into account.) */
    public abstract void cycleGetFrame(int frameIdx, Image canvas);

    /** Returns the index of the currently selected frame in the active cycle. */
    public abstract int cycleGetFrameIndex();
    /** Selects the specified frame in the active cycle. */
    public abstract boolean cycleSetFrameIndex(int frameIdx);

    /** Translates the active cycle's frame index into an absolute frame index. Returns -1 if cycle doesn't contain frames. */
    public abstract int cycleGetFrameIndexAbsolute();
    /** Translates the specified active cycle's frame index into an absolute frame index. Returns -1 if cycle doesn't contain frames. */
    public abstract int cycleGetFrameIndexAbsolute(int frameIdx);
    /** Translates the cycle's frame index into an absolute frame index. Returns -1 if cycle doesn't contain frames. */
    public abstract int cycleGetFrameIndexAbsolute(int cycleIdx, int frameIdx);


    // Updates the shared canvas size for the current BAM
    protected void updateSharedBamSize()
    {
      sharedBamSize = calculateSharedBamSize(sharedBamSize, isSharedPerCycle(), false);
    }

    // Calculates a shared canvas size for the current BAM.
    // cycleBased: true=for current cycle only, false=for all available frames
    // isMirrored: true=mirror along the x axis, false=no mirroring
    // To get the top-left corner of the selected frame:
    // For unmirrored frames:
    //   left = -sharedBamSize.x - frameCenterX()
    //   top  = -sharedBamSize.y - frameCenterY()
    // For mirrored frames:
    //   left = -sharedBamSize.x - (frameWidth() - frameCenterX() - 1)
    //   top  = -sharedBamSize.y - frameCenterY()
    protected Rectangle calculateSharedBamSize(Rectangle rect, boolean cycleBased, boolean isMirrored)
    {
      if (rect == null) {
        rect = new Rectangle();
      }

      int x1 = Integer.MAX_VALUE, x2 = Integer.MIN_VALUE;
      int y1 = Integer.MAX_VALUE, y2 = Integer.MIN_VALUE;
      if (cycleBased) {
        for (int i = 0; i < cycleFrameCount(); i++) {
          int frame = cycleGetFrameIndexAbsolute(i);
          int cx = isMirrored ? (parent.getFrameInfo(frame).getWidth() - parent.getFrameInfo(frame).getCenterX() - 1) : parent.getFrameInfo(frame).getCenterX();
          x1 = Math.min(x1, -cx);
          y1 = Math.min(y1, -parent.getFrameInfo(frame).getCenterY());
          x2 = Math.max(x2, parent.getFrameInfo(frame).getWidth() - cx);
          y2 = Math.max(y2, parent.getFrameInfo(frame).getHeight() - parent.getFrameInfo(frame).getCenterY());
        }
      } else {
        for (int i = 0; i < parent.frameCount(); i++) {
          int cx = isMirrored ? (parent.getFrameInfo(i).getWidth() - parent.getFrameInfo(i).getCenterX() - 1) : parent.getFrameInfo(i).getCenterX();
          x1 = Math.min(x1, -cx);
          y1 = Math.min(y1, -parent.getFrameInfo(i).getCenterY());
          x2 = Math.max(x2, parent.getFrameInfo(i).getWidth() - cx);
          y2 = Math.max(y2, parent.getFrameInfo(i).getHeight() - parent.getFrameInfo(i).getCenterY());
        }
      }
      if (x1 == Integer.MAX_VALUE) x1 = 0;
      if (y1 == Integer.MAX_VALUE) y1 = 0;
      if (x2 == Integer.MIN_VALUE) x2 = 0;
      if (y2 == Integer.MIN_VALUE) y2 = 0;
      rect.x = x1;
      rect.y = y1;
      rect.width = x2 - x1 + 1;
      rect.height = y2 - y1 + 1;

      return rect;
    }

    // Returns the shared rectangle object
    protected Rectangle getSharedRectangle()
    {
      return sharedBamSize;
    }
  }
}
// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Image;

/**
 * A structure to hold a unique identifier and the associated icon image.
 * @author argent77
 */
public class ResourceIcon extends BasicResource
{
  private final Image[] image;

  public ResourceIcon(String key, Image[] image)
  {
    super(key);
    this.image = image;
  }

  @Override
  public Image[] getData()
  {
    return image;
  }
}

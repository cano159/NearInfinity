// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.maze;

import java.nio.ByteBuffer;

import javax.swing.JComponent;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.TextString;
import org.infinity.gui.StructViewer;
import org.infinity.gui.hexview.BasicColorMap;
import org.infinity.gui.hexview.StructHexViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Resource;
import org.infinity.resource.gam.ModronMaze;
import org.infinity.resource.key.ResourceEntry;

/**
 * The PST:EE MAZE resource type.
 */
public class MazeResource extends AbstractStruct implements Resource, HasViewerTabs
{
  private static final String[] s_noyes = {"No", "Yes"};

  private StructHexViewer hexViewer;

  public MazeResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new TextString(buffer, offset + 4, 4, COMMON_VERSION));

    // TODO: confirm maze file structure
    int curOfs = offset + 8;

    // adding room data
    for (int i = 0; i < 64; i++) {
      MazeEntry entry = new MazeEntry(this, buffer, curOfs, i);
      addField(entry);
      curOfs += entry.getSize();
    }

    // adding header data
    addField(new DecNumber(buffer, curOfs, 4, ModronMaze.GAM_MAZE_SIZE_X));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, ModronMaze.GAM_MAZE_SIZE_Y));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, ModronMaze.GAM_MAZE_WIZARD_ROOM_X));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, ModronMaze.GAM_MAZE_WIZARD_ROOM_Y));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, ModronMaze.GAM_MAZE_NORDOM_X));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, ModronMaze.GAM_MAZE_NORDOM_Y));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, ModronMaze.GAM_MAZE_FOYER_X));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, ModronMaze.GAM_MAZE_FOYER_Y));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, ModronMaze.GAM_MAZE_ENGINE_ROOM_X));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, ModronMaze.GAM_MAZE_ENGINE_ROOM_Y));
    curOfs += 4;
    addField(new DecNumber(buffer, curOfs, 4, ModronMaze.GAM_MAZE_NUM_TRAPS));
    curOfs += 4;
    addField(new Bitmap(buffer, curOfs, 4, ModronMaze.GAM_MAZE_INITIALIZED, s_noyes));
    curOfs += 4;
    addField(new Bitmap(buffer, curOfs, 4, ModronMaze.GAM_MAZE_MAZE_BLOCKER_MADE, s_noyes));
    curOfs += 4;
    addField(new Bitmap(buffer, curOfs, 4, ModronMaze.GAM_MAZE_ENGINE_BLOCKER_MADE, s_noyes));
    curOfs += 4;

    return curOfs;
  }

//--------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount()
  {
    return 1;
  }

  @Override
  public String getViewerTabName(int index)
  {
    return StructViewer.TAB_RAW;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    if (hexViewer == null) {
      BasicColorMap colorMap = new BasicColorMap(this, true);
      colorMap.setColoredEntry(BasicColorMap.Coloring.BLUE, MazeEntry.class);
      hexViewer = new StructHexViewer(this, colorMap);
    }
    return hexViewer;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return false;
  }

//--------------------- End Interface HasViewerTabs ---------------------

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    viewer.addTabChangeListener(hexViewer);
  }
}

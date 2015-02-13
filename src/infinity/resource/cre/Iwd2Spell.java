// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.cre;

import infinity.datatype.DecNumber;
import infinity.datatype.IwdRef;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class Iwd2Spell extends AbstractStruct implements AddRemovable
{
  public Iwd2Spell() throws Exception
  {
    super(null, "Spell", new byte[16], 0);
  }

  public Iwd2Spell(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Spell", buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new IwdRef(buffer, offset, "ResRef", "LISTSPLL.2DA"));
    addField(new DecNumber(buffer, offset + 4, 4, "# memorizable"));
    addField(new DecNumber(buffer, offset + 8, 4, "# remaining"));
    addField(new Unknown(buffer, offset + 12, 4));
    return offset + 16;
  }
}


package validen.ast

import io.voklim.test._

class VValueTest extends UnitPropertySpec {

  property("should successfully validate the shape of a float"){
    VDbl.conforms(22.22f, (2, 2))
  }

}

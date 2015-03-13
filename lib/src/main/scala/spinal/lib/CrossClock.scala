package spinal.lib

import spinal.core
import spinal.core._


object BufferCC {
  def apply[T <: Data](input: T): T = apply(input, null.asInstanceOf[T])
  def apply[T <: Data](input: T, init: T): T = apply(input, init, 2)
  def apply[T <: Data](input: T, init: T, bufferDepth: Int): T = {
    val c = new BufferCC(input, init != null, bufferDepth)
    c.io.input := input
    if(init != null) c.io.init := init

    val ret = core.cloneOf(c.io.output)
    ret := c.io.output
    return ret
  }
}

class BufferCC[T <: Data](dataType: T, withInit : Boolean, bufferDepth: Int) extends Component {
  assert(bufferDepth >= 1)

  val io = new Bundle {
    val input = core.in(core.cloneOf(dataType))
    val init = if(!withInit) null.asInstanceOf[T] else core.in(dataType.clone)
    val output = core.out(dataType.clone)
  }

  val buffers = Vec(bufferDepth, Reg(dataType, io.init))

  buffers(0) := io.input
  buffers(0).addTag(core.crossClockDomain)
  for (i <- 1 until bufferDepth) {
    buffers(i) := buffers(i - 1)
    buffers(i).addTag(core.crossClockBuffer)

  }

  io.output := buffers.last


}


object PulseCCByToggle {
  def apply(input: Bool, clockIn: ClockDomain, clockOut: ClockDomain): Bool = {
    val c = new PulseCCByToggle(clockIn,clockOut)
    c.io.input := input
    return c.io.output

  }

}


class PulseCCByToggle(clockIn: ClockDomain, clockOut: ClockDomain) extends Component{
  val io = new Bundle{
    val input = core.in Bool()
    val output = core.in Bool()
  }
  val inputArea = new ClockingArea(clockIn) {
    val target = RegInit(Bool(false))
    when(io.input) {
      target := !target
    }
  }

  val outputArea = new ClockingArea(clockOut) {
    val target = BufferCC(inputArea.target, core.Bool(false))
    val hit = core.RegInit(core.Bool(false));

    core.when(target !== hit) {
      hit := !hit
    }

    io.output := (target !== hit)
  }
}
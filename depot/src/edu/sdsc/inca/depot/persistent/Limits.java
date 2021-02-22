package edu.sdsc.inca.depot.persistent;

import org.apache.xmlbeans.XmlObject;

/**
 * Basic Limits class; matches the idea of Limits in the xsd for inca.
 */
public class Limits extends XmlBeanObject {

  private Float memory;
  private Float cpuTime;
  private Float wallClockTime;

  public Limits() {
    this(-1, -1, -1);
  }

  /**
   * This constructor mainly helps out with testing; it won't be used in
   * coding, because we will be using the set function.
   * @param memory the limit on memory, in MB
   * @param cpuTime the limit on CPU time, in seconds
   * @param wallClockTime the limit on wall time, in seconds
   */
  public Limits(int memory, int cpuTime, int wallClockTime) {
    this(Float.valueOf(memory), Float.valueOf(cpuTime), Float.valueOf(wallClockTime));
  }

  /**
   *
   * @param memory
   * @param cpuTime
   * @param wallClockTime
   */
  public Limits(Float memory, Float cpuTime, Float wallClockTime) {
    this.memory = memory;
    this.cpuTime = cpuTime;
    this.wallClockTime = wallClockTime;
  }

  /**
   * Copies information from an Inca schema XmlBean Limits object so that this
   * object contains equivalent information.
   *
   * @param o the XmlBean Limits object to copy
   * @return this, for convenience
   */
  @Override
  public XmlBeanObject fromBean(XmlObject o) {
    return this.fromBean((edu.sdsc.inca.dataModel.util.Limits)o);
  }

  /**
   * Copies information from an Inca schema XmlBean Limits object so that this
   * object contains equivalent information.
   *
   * @param l the XmlBean Limits object to copy
   * @return this, for convenience
   */
  public Limits fromBean(edu.sdsc.inca.dataModel.util.Limits l) {
    this.setMemory(Float.valueOf(l.getMemory() == null ? "-1.0" : l.getMemory()));
    this.setCpuTime(Float.valueOf(l.getCpuTime()==null ? "-1.0" : l.getCpuTime()));
    this.setWallClockTime
      (Float.valueOf(l.getWallClockTime() == null ? "-1.0" : l.getWallClockTime()));
    return this;
  }

  public Float getMemory() {
    return memory;
  }

  public void setMemory(Float memory) {
    this.memory = memory;
  }

  public Float getCpuTime() {
    return cpuTime;
  }

  public void setCpuTime(Float cpuTime) {
    this.cpuTime = cpuTime;
  }

  public Float getWallClockTime() {
    return wallClockTime;
  }

  public void setWallClockTime(Float wallClockTime) {
    this.wallClockTime = wallClockTime;
  }

  /**
   * Returns a Inca schema XmlBean Limits object that contains information
   * equivalent to this object.
   *
   * @return an XmlBean Limits object that contains equivalent information
   */
  @Override
  public XmlObject toBean() {
    edu.sdsc.inca.dataModel.util.Limits result =
      edu.sdsc.inca.dataModel.util.Limits.Factory.newInstance();
    result.setMemory(this.getMemory().toString());
    result.setCpuTime(this.getCpuTime().toString());
    result.setWallClockTime(this.getWallClockTime().toString());
    return result;
  }

}

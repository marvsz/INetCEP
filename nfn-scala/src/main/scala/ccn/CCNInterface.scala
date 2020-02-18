package ccn

import ccn.packet.{CCNPacket, Content, Interest, ConstantInterest, RemoveConstantInterest}
import ccnlite.SensorSettings

import scala.concurrent.{ExecutionContext, Future}

trait CCNInterface {
  def wireFormat: CCNWireFormat
  def mkBinaryInterest(interest: Interest)(implicit ec: ExecutionContext): Future[Array[Byte]]
  def mkBinaryConstantInterest(constInterest: ConstantInterest)(implicit ec: ExecutionContext): Future[Array[Byte]]
  def mkBinaryRemoveConstantInterest(rmvconstInterest: RemoveConstantInterest)(implicit ec: ExecutionContext): Future[Array[Byte]]
  def mkBinaryContent(content: Content)(implicit ec: ExecutionContext): Future[List[Array[Byte]]]
  def mkBinaryDatastreamContent(content: Content)(implicit ec: ExecutionContext): Future[List[Array[Byte]]]
  def wireFormatDataToXmlPacket(binaryPacket: Array[Byte])(implicit ec: ExecutionContext): Future[CCNPacket]
  def addToCache(content: Content, mgmtSock: String)(implicit ec: ExecutionContext): Future[Int]
  def addDataStreamToCache(content: Content, mgmtSock: String) (implicit ec: ExecutionContext): Future[Int]
  def addSensor(sensorSettings: SensorSettings)(implicit ec: ExecutionContext) : Future[Int]
  def removeSensor(name: String, id: Int)(implicit ec: ExecutionContext): Future[Int]
}

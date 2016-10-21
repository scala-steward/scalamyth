package mythtv

package object model {
  type PropertyMap = Map[String, Any]
  type Guide = Iterable[ProgramGuideEntry]
  type MythFileHash = String
}

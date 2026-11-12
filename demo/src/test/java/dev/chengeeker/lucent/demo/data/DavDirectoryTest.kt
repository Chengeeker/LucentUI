package dev.chengeeker.lucent.demo.data
import org.junit.Assert.*
import org.junit.Test
class DavDirectoryTest {
 @Test fun preservesExistingDirectoryAndEncodesNewName() {
  assertEquals("https://example.com/dav/",davBaseUrl("https://example.com/dav","").toString())
  assertEquals("https://example.com/dav/my%20backup/",davBaseUrl("https://example.com/dav/","my backup").toString())
  assertEquals("https://example.com/dav/a%252Fb/",davBaseUrl("https://example.com/dav/","a%2Fb").toString())
 }
 @Test fun rejectsTraversalCredentialsAndNonHttps() {
  listOf("..","a/b","a\\b","bad\nname").forEach { name -> assertThrows(IllegalArgumentException::class.java){davBaseUrl("https://example.com/",name)} }
  listOf("http://example.com/","https://user:password@example.com/","https://example.com/?a=1","https://example.com/#fragment").forEach { url -> assertThrows(IllegalArgumentException::class.java){davBaseUrl(url,"")} }
 }
}

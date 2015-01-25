public class Test
{
#if FALSE
  INVALID
#endif

#if false
  INVALID
#endif

#if TRUE
  private void TestTrue() { }
#endif

#if true
  private void TestTrue2() { }
#endif

#define AAA
#if AAA
  private void Test1() {}
#else
  INVALID
#endif
#undef AAA
#if AAA
  INVALID
#else
  private void Test2() {}
#endif

#if AAA
  INVALID
#elif BBB
  private void Test3() {}
#else
  INVALID
#endif

}

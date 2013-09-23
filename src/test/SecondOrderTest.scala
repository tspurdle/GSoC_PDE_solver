object SecondOrderTest extends App {
  import pde._
  import pde.model.expression._
  import pde.model.{RectBoundary}
  import pde.model.expression.Expr.{double2Const, ciT2BFunction}
  import scala.math.Pi
  
  import pde.solver.Solver

  val x = new Variable("x")
  val t = Variable("t")
  val u = new FunctionVariable("u", x, t)
  val laplace = dd(u, x, x) + dd(u, t, t) := Const(0)
  val boundary = new RectBoundary(
    (u(x, 0) := 0, from(0 to 20)),
    (u(x, 10):=Sin(Pi * x/20)*5, from(0 to 20)),
    (u(0, t) :=0, from(0 to 10)),
    (u(20, t):=0, from(0 to 10))
  )
  val solution= Solver.solve(laplace, boundary, xstep = 0.2, tstep = 0.4)

  val realLSolution = (x: Double, t: Double) =>
    (5/scala.math.sinh(Pi/2)) * scala.math.sin(Pi * x / 20) * scala.math.sinh(Pi*t/20)
  
  println("Generated Point (4, 2): " + solution(4, 2) +"\n"+solution(4,2).d)
  println("Real: " + realLSolution(4,2))
  println("Generated Point(18, 9): " + solution(18, 9) +"\n"+solution(18,9).d)
  println("Real: " + realLSolution(18,9))
  println("Generated Point(10, 5): " + solution(10, 5)+"\n"+solution(10,5).d )
  println("Real: " + realLSolution(10,5))
  println("Generated Point(19.8, 9.8): " + solution(19.6, 9.8)+"\n"+solution(19.8, 9.8).d )
  println("Real: " + realLSolution(19.6,9.8))

  

}

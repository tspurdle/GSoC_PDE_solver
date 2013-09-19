//TBD

// For the reference, a first order PDE is
// a du/dt + b du/dx + c u + f = 0
package pde.solver
import pde.model._
import pde.model.expression._
import ceres.smartfloat.{SmartFloat}
import pde.util.{SparseMatrix, SparseVector}
import pde.model.errorData.ErrorData
class InvalidRowException extends Exception
class NotImplementedException extends Exception
object Solver {
  private val maxIterations = 50

  def solveDouble(eqn: LinearPDE1, boundary: RectBoundary):Array[Array[Double]] = {
    import scala.collection.immutable.Map
    import eqn.{function => f}
    import boundary.{bottom, top, left, right}
    
    val xstep = 0.1
    val tstep = 0.1
    val xsize : Int = ((bottom.interval.hi - bottom.interval.lo) / xstep + 1).toInt
    val tsize : Int = ((left.interval.hi - left.interval.lo) / tstep + 1).toInt
    val xrange : Int = xsize - 1
    val trange : Int = tsize -1
    val xmin  = boundary.xmin
    val tmin  = boundary.tmin
    
    var temp = Array.ofDim[Double](xsize, tsize)
    
    // Boundary Values
    var solution = generateRectBorderD(boundary,xstep, tstep, xsize, tsize)
    // println("xsize: " + xsize + ", tsize: " + tsize + ", xstep: "+ xstep + ", tstep: " + tstep +
    //         ", xmin: " + xmin + ", tmin: " + tmin)
    // The functions preceding dx, dt, u and f, so a, b, c and d
    // evaluated at points in the area
    val dtVals = generateFunctionValsD(eqn.dt, f, xstep, tstep, xsize, tsize, xmin, tmin)
    val dxVals = generateFunctionValsD(eqn.dx, f, xstep, tstep, xsize, tsize, xmin, tmin)
    val noOrderVals = generateFunctionValsD(eqn.noOrder, f, xstep, tstep, xsize, tsize, xmin, tmin)
    val noFunctVals = generateFunctionValsD(eqn.noFunct, f, xstep, tstep, xsize, tsize, xmin, tmin)
    val quotient = {
      for (i <- 0 until xsize)
      yield 
        for(j <- 0 until tsize)
        yield 1/(dtVals(i)(j)/tstep + dxVals(i)(j)/xstep + noOrderVals(i)(j))
    }
    var iterations = 0
    def generateSolutionApp() {
      var diff = 0.0
      def uij(i: Int)(j: Int): Double = {
        if (quotient(i)(j) != 0.0) {
          val centralValue = (dtVals(i)(j)/tstep * solution(i)(j-1) +
                              dxVals(i)(j)/xstep * solution(i-1)(j) -
                              noFunctVals(i)(j)) * quotient(i)(j)
          
          
          
          centralValue
        }
        else 0.0
      }
      
      for{i <- 1 until xsize-1;
          j <- 1 until tsize-1}{
            temp(i)(j) = uij(i)(j)
            if (temp(i)(j) != 0.0){
              val temp2 = SmartFloat.abs((temp(i)(j) - solution(i)(j)) / temp(i)(j))
              if (temp2 > diff) {diff = temp2.d}
            }
          }
      
      for (i <- 1 until xsize-1; j <- 1 until tsize-1)
        solution(i)(j) = temp(i)(j)
      

      // println("iters " +iterations +", point " + solution(1)(1))
      
      if (diff > 0.01 && iterations < maxIterations){
        iterations += 1
        generateSolutionApp
      }
      
    }
    generateSolutionApp()
    
    solution
  }
  
  def solve(eqn: PDE, boundary: Boundary,
            xstep: Double = 0.1, tstep: Double = 0.1): ErrorData = eqn match {
    case order1: PDE1 => solveOrder1(order1, boundary)(xstep, tstep)
    case order2: PDE2 => solveOrder2(order2, boundary)(xstep, tstep)
  }
  
  private def solveOrder1(eqn: PDE1, boundary: Boundary)
  (xstep: Double = 1, tstep: Double = 1) = (eqn, boundary) match {
    case (linear1: LinearPDE1, rb: RectBoundary) =>
      solveRectangularBoundary1(linear1, rb)(xstep, tstep)
    case _ => throw new CannotEvaluateException
  }
  
  private def solveRectangularBoundary1(eqn: LinearPDE1, boundary: RectBoundary)
  (xstep: Double = 1, tstep: Double = 1) = {
    import scala.collection.immutable.Map
    import eqn.{function => f}
    import boundary.{bottom, top, left, right}
    
    val xsize : Int = ((bottom.interval.hi - bottom.interval.lo) / xstep + 1).toInt
    val tsize : Int = ((left.interval.hi - left.interval.lo) / tstep + 1).toInt
    val xrange : Int = xsize - 1
    val trange : Int = tsize -1
    val xmin  = bottom.interval.lo
    val tmin  = left.interval.lo
    
    var temp = Array.ofDim[SmartFloat](xsize, tsize)
    
    // Boundary Values
    var solution = generateRectBorder(boundary,xstep, tstep, xsize, tsize)
    // println("xsize: " + xsize + ", tsize: " + tsize + ", xstep: "+ xstep + ", tstep: " + tstep +
    //         ", xmin: " + xmin + ", tmin: " + tmin)
    // The functions preceding dx, dt, u and f, so a, b, c and d
    // evaluated at points in the area
    val dtVals = generateFunctionVals(eqn.dt, f, xstep, tstep, xsize, tsize, xmin, tmin)
    val dxVals = generateFunctionVals(eqn.dx, f, xstep, tstep, xsize, tsize, xmin, tmin)
    val noOrderVals = generateFunctionVals(eqn.noOrder, f, xstep, tstep, xsize, tsize, xmin, tmin)
    val noFunctVals = generateFunctionVals(eqn.noFunct, f, xstep, tstep, xsize, tsize, xmin, tmin)
    val quotient = {
      for (i <- 0 until xsize)
      yield 
        for(j <- 0 until tsize)
        yield 1/(dtVals(i)(j)/tstep + dxVals(i)(j)/xstep + noOrderVals(i)(j))
    }
    println("Generating Solution")
    var iterations = 0
    def generateSolutionApp() {
      var diff = 0.0
      def uij(i: Int)(j: Int): SmartFloat = {
        if (quotient(i)(j) != 0.0) {
          val centralValue = (dtVals(i)(j)/tstep * solution(i)(j-1) +
                              dxVals(i)(j)/xstep * solution(i-1)(j) -
                              noFunctVals(i)(j)) * quotient(i)(j)
          
          
          val xError = - dxVals(i)(j) * ((xstep / 2) *
                                         (solution(i+1)(j) - 2 * solution(i)(j)
                                          + solution(i-1)(j)))
          
          val tError = - dtVals(i)(j) * ((tstep / 2) *
                                         (solution(i)(j+1) - 2 * solution(i)(j)
                                          + solution(i)(j-1)))
          
          centralValue addError xError addError tError
        }
        else SmartFloat(0.0)
      }
      
      for{i <- 1 until xsize-1;
          j <- 1 until tsize-1}{
            temp(i)(j) = uij(i)(j)
            if (temp(i)(j) != 0.0){
              val temp2 = ((temp(i)(j).d - solution(i)(j).d)/ temp(i)(j).d).abs
              if (temp2 > diff) {diff = temp2}
            }
          }
      
      for (i <- 1 until xsize-1; j <- 1 until tsize-1)
        solution(i)(j) = temp(i)(j)
      
      
      // println("iters " +iterations +", point " + solution(1)(1))
      
      if (diff > 0.01 && iterations < maxIterations){
        iterations += 1
        if (iterations % 10 == 0) println(iterations)
        generateSolutionApp
      }
    }
    generateSolutionApp()
    
    new ErrorData(solution, boundary)
    
  }
  
  //order 2
  
  private def solveOrder2(eqn: PDE2, boundary: Boundary)
  (xstep: Double = 0.01, tstep: Double = 0.01) = eqn match {
    case linear2: LinearPDE2 => (linear2, boundary) match {
      case (laplace2: Laplace2, rb: RectBoundary) => solveLaplace2(laplace2, rb)(xstep,tstep)
      case (scl: ScalarLinear2, rb: RectBoundary) => solveScalarLinear2(scl, rb)(xstep)
      case (heqn: ScalarHeatEquation, tb: ThreeSidedBoundary) => solveScalarHeatEquation(heqn, tb)
      case (_, rb: RectBoundary) => solveGeneralLinear2(linear2, rb)(xstep, tstep)
      case _ => throw new NotImplementedException
    }
  }
  
  private def solveLaplace2(eqn: Laplace2, boundary: RectBoundary)
  (xstep: Double = 0.01, tstep: Double = 0.01) = {
    import scala.collection.immutable.Map
    import eqn.{function => f}
    import boundary.{bottom, top, left, right}
    
    val xsize = ((bottom.interval.hi - bottom.interval.lo) / xstep + 1).toInt
    val tsize = ((left.interval.hi - left.interval.lo) / tstep + 1).toInt
    val xmin  = boundary.xmin
    val tmin  = boundary.tmin
    
    var temp = Array.ofDim[SmartFloat](xsize, tsize)
    var solution = generateRectBorder(boundary,xstep, tstep, xsize, tsize)
    var iterations = 0
    def generateSolutionApp(){
      var diff = 0.0
      def uij(i: Int)(j: Int) = {
        ((solution(i-1)(j) + solution(i+1)(j))/(xstep*xstep)
         +(solution(i)(j-1) + solution(i)(j+1))/(tstep*tstep))/(2*((xstep*xstep
                                                                    +tstep*tstep)/(xstep*xstep*tstep*tstep)))
      }
      for{i <- 1 until xsize-1;
          j <- 1 until tsize-1}{
            temp(i)(j) = uij(i)(j)
            val rDiff = SmartFloat.abs((temp(i)(j) - solution(i)(j))/temp(i)(j))
            if (rDiff > diff) diff = rDiff.d
          }
      
      for (i <- 1 until xsize-1; j <- 1 until tsize-1)
        solution(i)(j) = temp(i)(j)
      
      if (diff > 0.00001 && iterations < 100000){
        iterations += 1
        generateSolutionApp()
      }
      
    }
    
    
    generateSolutionApp()
    
    new ErrorData(solution, boundary)
  }
  
  private def solveScalarHeatEquation(eqn: ScalarHeatEquation,
                                      boundary: ThreeSidedBoundary) : ErrorData = {
    // Application of Crank-Nicolson's algorithm
    import scala.collection.immutable.Map
    import eqn.{function => f}
    import boundary.{bottom, left, right}
    import pde.util.TriDiagonalMatrix
    val xstep = (boundary.xmax - boundary.xmin)/ 1000
    val tstep = (boundary.tmax - boundary.tmin) / 1000
    println(xstep + ", " + tstep)
    val xsize = 1000
    val tsize = 1000
    val xmin  = bottom.interval
    val tmin  = left.interval.lo
    
    var temp = Array.ofDim[SmartFloat](xsize, tsize)
    
    // Inputting the Boundary Values
    val solution: Array[Array[SmartFloat]] = generateThreeSidedBorder(boundary,xstep, tstep, xsize, tsize)
    
    val r = eqn.a * tstep /(2 * xstep * xstep)
    val mid = Vector.fill(xsize-1)(1+2*r)
    val others = Vector.fill(xsize-2)(-r)
    val aMatrix = TriDiagonalMatrix(mid, others, others)
    def nextRow(n: Int) = {
      def Di(i: Int) = {
        val centralValue = r * solution(i)(n) + (1-2*r) * solution(i)(n) + r*solution(i)(n)
        val error = (0.5 * tstep + (1/12) * xstep * xstep)*solution(i)(n)
        centralValue addError error
      }
      val D1 = r * (solution(0)(n) + solution(0)(n+1)) + (1-2*r)*solution(1)(n) + r * solution(2)(n)
      val Dn = (1-2*r)*solution(xsize-2)(n) + r * solution(xsize-1)(n) + r*(solution(xsize-3)(n))
      val Dis = {
        for(i<- 2 until xsize-2)
        yield Di(i)
      }
      aMatrix.solveSystem(D1 +: Dis :+ Dn)
    }
    println("Solving")
    for(i <- 0 until tsize-1){
      val nRow = nextRow(i)
      if (i%50==0) println(i)
      for(j <- 1 until xsize-1){
        solution(j)(i+1) = nRow(j-1)
      }
    }
    // val Ai = - tstep * (2 * eqn.a + xstep * eqn.b)
    // val Bi = 2* (2 * xstep * xstep + 2 * tstep * eqn.a - xstep * xstep * tstep * eqn.c)
    // val Ci = - tstep * (2 * eqn.a - xstep * eqn.b)
    // // Vector(Ai, Bi, Ci).foreach(x => println(x.d + " "))
    // val upper = Vector.fill(xsize-2)(Ci)
    // val main = Vector.fill(xsize-1)(Bi)
    // val lower = Vector.fill(xsize - 2)(Ai)
    // val aMatrix = new TriDiagonalMatrix(main, upper, lower)
    // val fnl = left.u.exp.expr2Function2
    // val fnr = right.u.exp.expr2Function2
    // def nextRow(n: Int) : IndexedSeq[SmartFloat] = {
    //   if (n < 0) throw new InvalidRowException
    //   else {
    //     def Di(i: Int): SmartFloat = {
    //       val centralValue = -Ai * solution(i+1)(n) - Bi * solution(i)(n) - Ci * solution(i-1)(n)
    //       val dxxError = (0.5 * tstep + (1/12) * xstep * xstep)
    //         centralValue
    //     }
    //     val Dis = {
    //       for(i<- 2 until xsize-2)
    //       yield Di(i)
    //     }
    //     val D1 = - Ci * (solution(0)(n) + solution(0)(n+1)) - Bi * solution(1)(n) - Ai * solution(2)(n)
    
    //     val Dn = - Ai * (solution(xsize-1)(n) + solution(xsize-1)(n+1) ) - (
    //       Bi * solution(xsize-2)(n) + Ci * solution(xsize-3)(n))
    
    //      println("n " + n)
    //      println(solution(0)(n).d + ", " + solution(1)(n).d + ", "
    //              + solution(xsize-2)(n).d+ ", " + solution(xsize-1)(n).d)
    //      println("Di")
    //      (D1 +: Dis :+ Dn).foreach( x => print(x.d + ", "))
    //      println("")
    //      println("n+1")
    //      aMatrix.solveSystem(D1 +: Dis :+ Dn).foreach(x => print(x.d + ", "))
    //      println("")
    //     aMatrix.solveSystem(D1 +: Dis :+ Dn)
    //   }
    // }
    // def copyRow(n: Int, row: IndexedSeq[SmartFloat]) {
    //   for(i <- 1 until xsize)
    //     solution(i)(n) = row(i)
    // }
    
    // for(i <- 0 until tsize-1){
    //   val nRow = nextRow(i)
    //   if (i%50==0) println(i)
    //   for(j <- 1 until xsize-1){
    //     solution(j)(i+1) = nRow(j-1)
    //      print(solution(j)(i+1).d + ", ")
    //   }
    //    println("")
    // }
    new ErrorData(solution, boundary)
  }
  
  private def solveScalarLinear2(eqn: ScalarLinear2, boundary: RectBoundary)(step: Double = 0.01) = {
    import scala.collection.immutable.Map
    import eqn.{function => f}
    import boundary.{bottom, top, left, right}
    val xmax = bottom.interval.hi
    val xmin = bottom.interval.lo
    val tmax = left.interval.hi
    val tmin = left.interval.lo
    val size = ((xmax-xmin)/step).toInt
    val tstep = (tmax-tmin)/size
    val xstep = tstep
    
    var temp = Array.ofDim[SmartFloat](size, size)
    
    // Inputting the Boundary Values
    var solution = generateRectBorder(boundary,xstep, tstep, size, size)
    
    // The functions preceding dx, dt, u and f, so a, b, c and d
    // evaluated at points in the area
    val quotientZero = {
      for (i <- 0 until size)
      yield {
        for (j <- 0 until size)
        yield ((eqn.dtVal/tstep + eqn.dxVal/xstep + eqn.noOrderVal) == 0.0)
      }
    }
    
    /**
     * Remapping all points from (j, l) to i = j(L+1)+l
     * where L is the max index we iterate to, so size-1,
     * into a sparse matrix
     */
    val sparseMat = pde.util.SparseMatrix.zeroes
    def addVals : Unit = {
      def genRow(i: Int): SparseVector[SmartFloat] = {
        val row = new SparseVector[SmartFloat](size)
        val L = size-1
        if (i == 0) {
          import eqn.{dttVal, dxtVal, dxxVal, dtVal, dxVal, noOrderVal, noFunctVal}
          val u0 = - ((2*dxxVal/xstep+dxVal)/xstep+(2*dttVal/tstep+dtVal)/tstep -noOrderVal)
          val u1 = (dttVal/(tstep*tstep) + dtVal/tstep)
            val uLplus1 = (dxxVal/(xstep*xstep) + dxVal/xstep)
              val others = dxtVal/(xstep*tstep)
          row += (0, u0)
          row += (1, u1)
          row += (L+1, uLplus1)
          row += (L+2, others)
          row += (L, -others)
        }
        else if (i > L && L< size*size) ()
        
        row
      }
      
      for(i <- 0 until size; rowi = genRow(i))
        sparseMat += (i, rowi)
    }
    new ErrorData(solution, boundary)
  }

  
  private def solveGeneralLinear2(eqn: LinearPDE2, boundary: RectBoundary)
  (xstep: Double = 0.01, tstep: Double = 0.01)= {
    import scala.collection.immutable.Map
    import eqn.{function => f}
    import boundary.{bottom, top, left, right}
    val xsize = ((bottom.interval.hi - bottom.interval.lo) / xstep + 1).toInt
    val tsize = ((left.interval.hi - left.interval.lo) / tstep + 1).toInt
    val xmin  = bottom.interval.lo
    val tmin  = left.interval.lo
    
    var temp = Array.ofDim[SmartFloat](xsize, tsize)
    
    // Inputting the Boundary Values
    var solution = generateRectBorder(boundary,xstep, tstep, xsize, tsize)

    // The functions preceding dx, dt, u and f, so a, b, c and d
    // evaluated at points in the area
    val dttVals = generateFunctionVals(eqn.dtt, f, xstep, tstep, xsize, tsize, xmin, tmin)
    val dxtVals = generateFunctionVals(eqn.dxt, f, xstep, tstep, xsize, tsize, xmin, tmin)
    val dxxVals = generateFunctionVals(eqn.dxx, f, xstep, tstep, xsize, tsize, xmin, tmin)
    val dtVals = generateFunctionVals(eqn.dt, f, xstep, tstep, xsize, tsize, xmin, tmin)
    val dxVals = generateFunctionVals(eqn.dx, f, xstep, tstep, xsize, tsize, xmin, tmin)
    val noOrderVals = generateFunctionVals(eqn.noOrder, f, xstep, tstep, xsize, tsize, xmin, tmin)
    val noFunctVals = generateFunctionVals(eqn.noFunct, f, xstep, tstep, xsize, tsize, xmin, tmin)
    val quotientZero = {
      for (i <- 0 until xsize)
      yield {
        for (j <- 0 until tsize)
        yield ((dtVals(i)(j)/tstep + dxVals(i)(j)/xstep + noOrderVals(i)(j)) == 0.0)
      }
    }
    
    var iterations = 0
    val xstepSquared = xstep * xstep
    val tstepSquared = tstep * tstep
    def generateSolutionApp() {
      var diff = 0.0
      def uij(i: Int)(j: Int) = {
        import eqn._
        val xpoint = xmin+i*xstep
        val tpoint = tmin+j*tstep
        val evalPoint = Map(f.x -> xpoint, f.t -> tpoint)
        (xstepSquared*tstepSquared)*(dxxVals(i)(j)*(solution(i-1)(j)+solution(i+1)(j))/(xstepSquared)
                                     + dttVals(i)(j)*(solution(i)(j-1)+solution(i)(j+1))/(tstepSquared)
                                     +dxtVals(i)(j)*(solution(i+1)(j+1)
                                                     - solution(i+1)(j-1) - solution(i-1)(j+1)+solution(i+1)(j-1))/(4*xstep*tstep)
                                     +dxVals(i)(j)*(solution(i+1)(j))/xstep
                                     +dtVals(i)(j)*(solution(i)(j+1))/tstep
                                     +noFunctVals(i)(j))/
        (2*tstepSquared*dxxVals(i)(j)
         +2*xstepSquared*dttVals(i)(j)
         +xstep*tstepSquared*dxVals(i)(j)
         +xstepSquared*tstep*dtVals(i)(j)
         +xstepSquared*tstepSquared*noOrderVals(i)(j))
      }
      for{i <- 1 until xsize-1;
          j <- 1 until tsize-1}{
            temp(i)(j) = uij(i)(j)
            val temp2 = SmartFloat.abs((temp(i)(j) - solution(i)(j)) / temp(i)(j))
            if (temp2 > diff) {diff = temp2.d}
          }
      for (i <- 1 until xsize-1; j <- 1 until tsize-1)
        solution(i)(j) = temp(i)(j)
      
      
      if (diff > 0.000000001 && iterations < 10000){
        iterations += 1
        generateSolutionApp
      }
      
    }
    
    generateSolutionApp()
    new ErrorData(solution, boundary)
  }


  def generateRectBorder(boundary: RectBoundary, xstep: Double, tstep: Double,
                         xsize: Int, tsize: Int): Array[Array[SmartFloat]] = {
    import boundary.{bottom, top, left, right, f}
    var solution = Array.fill(xsize, tsize)(SmartFloat(0.0))
    for (i <- 0 until xsize){
      val xpoint = bottom.interval.lo + i*xstep
      val tpoint = left.interval.lo + i*tstep
      solution(i)(0) = bottom.u.exp.eval((f.x, xpoint), (f.t, bottom.u.c))
      solution(i)(tsize-1) = top.u.exp.eval((f.x, xpoint),(f.t, top.u.c))
      solution(0)(i) = left.u.exp.eval((f.x, left.u.c), (f.t, tpoint))
      solution(xsize-1)(i) = right.u.exp.eval((f.x, right.u.c), (f.t, tpoint))
    }
    
    solution
  }
  
  private def generateThreeSidedBorder(boundary: ThreeSidedBoundary, xstep: Double, tstep: Double,
                                       xsize: Int, tsize: Int): Array[Array[SmartFloat]] = {
    import boundary.{bottom, left, right, f}
    var solution = Array.fill(xsize, tsize)(SmartFloat(0.0))
    val fnb = bottom.u.exp.expr2Function2
    val fnl = left.u.exp.expr2Function2
    val fnr = right.u.exp.expr2Function2
    for (i <- 0 until xsize){
      val xpoint = bottom.interval.lo + i*xstep
      solution(i)(0) = fnb((f.x, xpoint), (f.t, bottom.u.c))
    }
    for (i <- 0 until tsize){
      val tpoint = bottom.u.c + i*tstep
      solution(0)(i) = fnl((f.x, left.u.c), (f.t, tpoint))
      solution(xsize-1)(i) = fnr((f.x, right.u.c),(f.t, tpoint))
    }
    solution
  }

  def generateFunctionVals(e: Expr, f: FunctionVariable,
                           xstep: Double, tstep: Double, xsize: Int, tsize: Int,
                           xmin: Double, tmin: Double): Array[Array[SmartFloat]] = e match {
    case Zero     => Array.fill(xsize,tsize)(SmartFloat(0.0))
    case Const(c) => Array.fill(xsize,tsize)(c)
    case _        => {
      val fn = e.expr2Function2
      val result = Array.ofDim[SmartFloat](xsize,tsize)
      for (i <- 0 until xsize) {
        for (j <- 0 until tsize) {
          val xpoint = xmin + i * xstep
          val tpoint = tmin + j * tstep
          val (evalx, evalt) = ((f.x, xpoint), (f.t, tpoint))
            result(i)(j) = fn(evalx, evalt)
        }
      }
      
      result
    }
  }

  def generateRectBorderD(boundary: RectBoundary, xstep: Double, tstep: Double,
                          xsize: Int, tsize: Int): Array[Array[Double]] = {
    import boundary.{bottom, top, left, right, f}
    var solution = Array.fill(xsize, tsize)((0.0))
    for (i <- 0 until xsize){
      val xpoint = bottom.interval.lo + i*xstep
      val tpoint = left.interval.lo + i*tstep
      solution(i)(0) = bottom.u.exp.eval((f.x, xpoint), (f.t, bottom.u.c)).d
      solution(i)(tsize-1) = top.u.exp.eval((f.x, xpoint),(f.t, top.u.c)).d
      solution(0)(i) = left.u.exp.eval((f.x, left.u.c), (f.t, tpoint)).d
        solution(xsize-1)(i) = right.u.exp.eval((f.x, right.u.c), (f.t, tpoint)).d
    }
    
    solution
  }

  def generateFunctionValsD(e: Expr, f: FunctionVariable,
                            xstep: Double, tstep: Double, xsize: Int, tsize: Int,
                            xmin: Double, tmin: Double): Array[Array[Double]] = e match {
    case Zero     => Array.fill(xsize,tsize)(0.0)
    case Const(c) => Array.fill(xsize,tsize)(c.d)
    case _        => {
      val fn = e.expr2Function2
      val result = Array.ofDim[Double](xsize,tsize)
      for (i <- 0 until xsize) {
        for (j <- 0 until tsize) {
          val xpoint = xmin + i * xstep
          val tpoint = tmin + j * tstep
          val (evalx, evalt) = ((f.x, xpoint), (f.t, tpoint))
            result(i)(j) = fn(evalx, evalt).d
        }
      }
      
      result
    }
  }
}

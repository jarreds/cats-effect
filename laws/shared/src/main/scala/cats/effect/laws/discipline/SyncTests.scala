/*
 * Copyright 2017 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cats
package effect
package laws
package discipline

import cats.data._
import cats.laws.discipline._
import cats.laws.discipline.SemigroupalTests.Isomorphisms

import org.scalacheck._, Prop.forAll

trait SyncTests[F[_]] extends MonadErrorTests[F, Throwable] with TestsPlatform {
  def laws: SyncLaws[F]

  def sync[A: Arbitrary: Eq, B: Arbitrary: Eq, C: Arbitrary: Eq](
    implicit
      ArbFA: Arbitrary[F[A]],
      ArbFB: Arbitrary[F[B]],
      ArbFC: Arbitrary[F[C]],
      ArbFU: Arbitrary[F[Unit]],
      ArbFAtoB: Arbitrary[F[A => B]],
      ArbFBtoC: Arbitrary[F[B => C]],
      ArbT: Arbitrary[Throwable],
      CogenA: Cogen[A],
      CogenB: Cogen[B],
      CogenC: Cogen[C],
      CogenT: Cogen[Throwable],
      EqFA: Eq[F[A]],
      EqFB: Eq[F[B]],
      EqFC: Eq[F[C]],
      EqFU: Eq[F[Unit]],
      EqT: Eq[Throwable],
      EqFEitherTU: Eq[F[Either[Throwable, Unit]]],
      EqFEitherTA: Eq[F[Either[Throwable, A]]],
      EqEitherTFTA: Eq[EitherT[F, Throwable, A]],
      EqFABC: Eq[F[(A, B, C)]],
      EqFInt: Eq[F[Int]],
      iso: Isomorphisms[F]): RuleSet = {
    new RuleSet {
      val name = "sync"
      val bases = Nil
      val parents = Seq(monadError[A, B, C])

      val baseProps = Seq(
        "delay constant is pure" -> forAll(laws.delayConstantIsPure[A] _),
        "suspend constant is pure join" -> forAll(laws.suspendConstantIsPureJoin[A] _),
        "throw in delay is raiseError" -> forAll(laws.delayThrowIsRaiseError[A] _),
        "throw in suspend is raiseError" -> forAll(laws.suspendThrowIsRaiseError[A] _),
        "unsequenced delay is no-op" -> forAll(laws.unsequencedDelayIsNoop[A] _),
        "repeated sync evaluation not memoized" -> forAll(laws.repeatedSyncEvaluationNotMemoized[A] _),
        "propagate errors through bind (suspend)" -> forAll(laws.propagateErrorsThroughBindSuspend[A] _))

      val jvmProps = Seq(
        "stack-safe on left-associated binds" -> Prop.lzy(laws.stackSafetyOnRepeatedLeftBinds),
        "stack-safe on right-associated binds" -> Prop.lzy(laws.stackSafetyOnRepeatedRightBinds),
        "stack-safe on repeated attempts" -> Prop.lzy(laws.stackSafetyOnRepeatedAttempts))

      val jsProps = Seq()

      val props = baseProps ++ (if (isJVM) jvmProps else jsProps)
    }
  }
}

object SyncTests {
  def apply[F[_]: Sync]: SyncTests[F] = new SyncTests[F] {
    def laws = SyncLaws[F]
  }
}

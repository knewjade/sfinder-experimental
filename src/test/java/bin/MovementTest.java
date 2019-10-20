package bin;

import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.srs.MinoRotation;
import core.srs.Rotate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MovementTest {
    private Movement createMovement() {
        MinoFactory minoFactory = new MinoFactory();
        MinoRotation minoRotation = MinoRotation.create();
        MinoShifter minoShifter = new MinoShifter();
        return new Movement(minoFactory, minoRotation, minoShifter);
    }

    @Test
    void T() {
        Movement movement = createMovement();

        assertThat(movement.harddrop(Piece.T, Rotate.Spawn, 1))
                .returns(3, Step::movement)
                .returns(0, Step::rotateCount);
        assertThat(movement.harddrop(Piece.T, Rotate.Spawn, 4))
                .returns(0, Step::movement)
                .returns(0, Step::rotateCount);
        assertThat(movement.harddrop(Piece.T, Rotate.Spawn, 8))
                .returns(4, Step::movement)
                .returns(0, Step::rotateCount);

        assertThat(movement.harddrop(Piece.T, Rotate.Right, 0))
                .returns(4, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.T, Rotate.Right, 4))
                .returns(0, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.T, Rotate.Right, 8))
                .returns(4, Step::movement)
                .returns(1, Step::rotateCount);

        assertThat(movement.harddrop(Piece.T, Rotate.Reverse, 1))
                .returns(3, Step::movement)
                .returns(2, Step::rotateCount);
        assertThat(movement.harddrop(Piece.T, Rotate.Reverse, 4))
                .returns(0, Step::movement)
                .returns(2, Step::rotateCount);
        assertThat(movement.harddrop(Piece.T, Rotate.Reverse, 8))
                .returns(4, Step::movement)
                .returns(2, Step::rotateCount);

        assertThat(movement.harddrop(Piece.T, Rotate.Left, 1))
                .returns(3, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.T, Rotate.Left, 4))
                .returns(0, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.T, Rotate.Left, 9))
                .returns(5, Step::movement)
                .returns(1, Step::rotateCount);
    }

    @Test
    void I() {
        Movement movement = createMovement();

        assertThat(movement.harddrop(Piece.I, Rotate.Spawn, 1))
                .returns(3, Step::movement)
                .returns(0, Step::rotateCount);
        assertThat(movement.harddrop(Piece.I, Rotate.Spawn, 4))
                .returns(0, Step::movement)
                .returns(0, Step::rotateCount);
        assertThat(movement.harddrop(Piece.I, Rotate.Spawn, 7))
                .returns(3, Step::movement)
                .returns(0, Step::rotateCount);

        assertThat(movement.harddrop(Piece.I, Rotate.Left, 0))
                .returns(4, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.I, Rotate.Left, 4))
                .returns(0, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.I, Rotate.Left, 5))
                .returns(0, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.I, Rotate.Left, 9))
                .returns(4, Step::movement)
                .returns(1, Step::rotateCount);
    }

    @Test
    void L() {
        Movement movement = createMovement();

        assertThat(movement.harddrop(Piece.L, Rotate.Spawn, 1))
                .returns(3, Step::movement)
                .returns(0, Step::rotateCount);
        assertThat(movement.harddrop(Piece.L, Rotate.Spawn, 4))
                .returns(0, Step::movement)
                .returns(0, Step::rotateCount);
        assertThat(movement.harddrop(Piece.L, Rotate.Spawn, 8))
                .returns(4, Step::movement)
                .returns(0, Step::rotateCount);

        assertThat(movement.harddrop(Piece.L, Rotate.Right, 0))
                .returns(4, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.L, Rotate.Right, 4))
                .returns(0, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.L, Rotate.Right, 8))
                .returns(4, Step::movement)
                .returns(1, Step::rotateCount);

        assertThat(movement.harddrop(Piece.L, Rotate.Reverse, 1))
                .returns(3, Step::movement)
                .returns(2, Step::rotateCount);
        assertThat(movement.harddrop(Piece.L, Rotate.Reverse, 4))
                .returns(0, Step::movement)
                .returns(2, Step::rotateCount);
        assertThat(movement.harddrop(Piece.L, Rotate.Reverse, 8))
                .returns(4, Step::movement)
                .returns(2, Step::rotateCount);

        assertThat(movement.harddrop(Piece.L, Rotate.Left, 1))
                .returns(3, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.L, Rotate.Left, 4))
                .returns(0, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.L, Rotate.Left, 9))
                .returns(5, Step::movement)
                .returns(1, Step::rotateCount);
    }

    @Test
    void J() {
        Movement movement = createMovement();

        assertThat(movement.harddrop(Piece.J, Rotate.Spawn, 1))
                .returns(3, Step::movement)
                .returns(0, Step::rotateCount);
        assertThat(movement.harddrop(Piece.J, Rotate.Spawn, 4))
                .returns(0, Step::movement)
                .returns(0, Step::rotateCount);
        assertThat(movement.harddrop(Piece.J, Rotate.Spawn, 8))
                .returns(4, Step::movement)
                .returns(0, Step::rotateCount);

        assertThat(movement.harddrop(Piece.J, Rotate.Right, 0))
                .returns(4, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.J, Rotate.Right, 4))
                .returns(0, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.J, Rotate.Right, 8))
                .returns(4, Step::movement)
                .returns(1, Step::rotateCount);

        assertThat(movement.harddrop(Piece.J, Rotate.Reverse, 1))
                .returns(3, Step::movement)
                .returns(2, Step::rotateCount);
        assertThat(movement.harddrop(Piece.J, Rotate.Reverse, 4))
                .returns(0, Step::movement)
                .returns(2, Step::rotateCount);
        assertThat(movement.harddrop(Piece.J, Rotate.Reverse, 8))
                .returns(4, Step::movement)
                .returns(2, Step::rotateCount);

        assertThat(movement.harddrop(Piece.J, Rotate.Left, 1))
                .returns(3, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.J, Rotate.Left, 4))
                .returns(0, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.J, Rotate.Left, 9))
                .returns(5, Step::movement)
                .returns(1, Step::rotateCount);
    }

    @Test
    void S() {
        Movement movement = createMovement();

        assertThat(movement.harddrop(Piece.S, Rotate.Spawn, 1))
                .returns(3, Step::movement)
                .returns(0, Step::rotateCount);
        assertThat(movement.harddrop(Piece.S, Rotate.Spawn, 4))
                .returns(0, Step::movement)
                .returns(0, Step::rotateCount);
        assertThat(movement.harddrop(Piece.S, Rotate.Spawn, 8))
                .returns(4, Step::movement)
                .returns(0, Step::rotateCount);

        assertThat(movement.harddrop(Piece.S, Rotate.Left, 1))
                .returns(3, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.S, Rotate.Left, 4))
                .returns(0, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.S, Rotate.Left, 5))
                .returns(0, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.S, Rotate.Left, 9))
                .returns(4, Step::movement)
                .returns(1, Step::rotateCount);
    }

    @Test
    void Z() {
        Movement movement = createMovement();

        assertThat(movement.harddrop(Piece.Z, Rotate.Spawn, 1))
                .returns(3, Step::movement)
                .returns(0, Step::rotateCount);
        assertThat(movement.harddrop(Piece.Z, Rotate.Spawn, 4))
                .returns(0, Step::movement)
                .returns(0, Step::rotateCount);
        assertThat(movement.harddrop(Piece.Z, Rotate.Spawn, 8))
                .returns(4, Step::movement)
                .returns(0, Step::rotateCount);

        assertThat(movement.harddrop(Piece.Z, Rotate.Right, 0))
                .returns(3, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.Z, Rotate.Right, 3))
                .returns(0, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.Z, Rotate.Right, 4))
                .returns(0, Step::movement)
                .returns(1, Step::rotateCount);
        assertThat(movement.harddrop(Piece.Z, Rotate.Right, 8))
                .returns(4, Step::movement)
                .returns(1, Step::rotateCount);
    }

    @Test
    void O() {
        Movement movement = createMovement();

        assertThat(movement.harddrop(Piece.O, Rotate.Spawn, 0))
                .returns(4, Step::movement)
                .returns(0, Step::rotateCount);
        assertThat(movement.harddrop(Piece.O, Rotate.Spawn, 4))
                .returns(0, Step::movement)
                .returns(0, Step::rotateCount);
        assertThat(movement.harddrop(Piece.O, Rotate.Spawn, 8))
                .returns(4, Step::movement)
                .returns(0, Step::rotateCount);
    }
}
package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.Range;


/**
 *
 */
@TeleOp(name="Tyler TeleOp", group="AAA")
public class TylerController extends OpMode {

    // Motors connected to the hub.
    private DcMotor motorBackLeft;
    private DcMotor motorBackRight;
    private DcMotor motorFrontLeft;
    private DcMotor motorFrontRight;

    // Hack stuff.
    private boolean useMotors = true;
    private boolean useEncoders = true;
    private boolean useArm = true; // HACK
    private boolean useLifter = true; // HACL
    private boolean useDropper = true;

    //Movement State
    private int armState;
    private int extenderTarget;
    private int lifterState;
    private int lifterExtenderTarget;
    private int extenderStartPostion = 0;
    private int lifterStartPosition = 0;
    private int shoulderTarget;
    private int shoulderStartPosition = 0;

    //Drive State
    private boolean switchFront = false;


    /**
     * Code to run ONCE when the driver hits INIT
     */
    @Override
    public void init() {

        // Initialize the motors.
        if (useMotors) {
            motorBackLeft = hardwareMap.get(DcMotor.class, "motor0");
            motorBackRight = hardwareMap.get(DcMotor.class, "motor1");
            motorFrontLeft = hardwareMap.get(DcMotor.class, "motor2");
            motorFrontRight = hardwareMap.get(DcMotor.class, "motor3");

            // Most robots need the motor on one side to be reversed to drive forward
            // Reverse the motor that runs backwards when connected directly to the battery
            motorBackLeft.setDirection(DcMotor.Direction.FORWARD);
            motorBackRight.setDirection(DcMotor.Direction.REVERSE);
            motorFrontLeft.setDirection(DcMotor.Direction.FORWARD);
            motorFrontRight.setDirection(DcMotor.Direction.REVERSE);

            if (useEncoders) {
                motorBackLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                motorBackRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                motorFrontLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                motorFrontRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

                motorBackRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                motorBackLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                motorFrontRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                motorFrontLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            }
        }

    }


    /**
     * Code to run REPEATEDLY after the driver hits INIT, but before they hit PLAY
     */
    @Override
    public void init_loop() {
    }

    /**
     * Code to run ONCE when the driver hits PLAY
     */
    @Override
    public void start() {

    }

    /**
     * Code to run ONCE after the driver hits STOP
     */
    @Override
    public void stop() {
    }

    /**
     * Code to run REPEATEDLY after the driver hits PLAY but before they hit STOP
     */
    @Override
    public void loop() {


        if (useMotors) {
            // Switch the directions for driving!
            if (gamepad1.b) {
                switchFront = !switchFront;
                sleep(500);
            }

            // Control the wheel motors.
            // POV Mode uses left stick to go forw ard, and right stick to turn.
            // - This uses basic math to combine motions and is easier to drive straight.
            double driveNormal = -gamepad1.left_stick_y;
            if (Math.abs(driveNormal) < 0.1)
                driveNormal = 0.0; // Prevent the output from saying "-0. 0".

            double driveStrafe = -gamepad1.left_stick_x;
            if (Math.abs(driveStrafe) < 0.1)
                driveStrafe = 0.0; // Prevent the output from saying "-0.0".

            double turn = gamepad1.right_stick_x;

            if (switchFront) {
                driveNormal = -driveNormal;
                driveStrafe = -driveStrafe;
            }
            telemetry.addData("Motor", "n:%02.1f, s:%02.1f, t:%02.1f", driveNormal, driveStrafe, turn);

            float cap = 1.0f;
            // float backScale = 0.5f;
            double leftBackPower = Range.clip(driveNormal + turn + (driveStrafe), -cap, cap);
            double rightBackPower = Range.clip(driveNormal - turn - (driveStrafe), -cap, cap);
            double leftFrontPower = Range.clip(driveNormal + turn - driveStrafe, -cap, cap);
            double rightFrontPower = Range.clip(driveNormal - turn + driveStrafe, -cap, cap);

            double halfLeftBackPower = Range.clip(driveNormal + turn + driveStrafe, -0.25, 0.25);
            double halfRightBackPower = Range.clip(driveNormal - turn - driveStrafe, -0.25, 0.25);
            double halfLeftFrontPower = Range.clip(driveNormal + turn - driveStrafe, -0.25, 0.25);
            double halfRightFrontPower = Range.clip(driveNormal - turn + driveStrafe, -0.25, 0.25);

            boolean halfSpeed = gamepad1.right_stick_button;
            if (halfSpeed) {
                motorBackLeft.setPower(halfLeftBackPower);
                motorBackRight.setPower(halfRightBackPower);
                motorFrontLeft.setPower(halfLeftFrontPower);
                motorFrontRight.setPower(halfRightFrontPower);
                telemetry.addData("Motor", "half lb:%02.1f, rb:%02.1f, lf:%02.1f, rf:%02.1f", halfLeftBackPower, halfRightBackPower, halfLeftFrontPower, halfRightFrontPower);
            } else {
                motorBackLeft.setPower(leftBackPower);
                motorBackRight.setPower(rightBackPower);
                motorFrontLeft.setPower(leftFrontPower);
                motorFrontRight.setPower(rightFrontPower);
                telemetry.addData("Motor", "full left-back:%02.1f, %d", leftBackPower, motorBackLeft.getCurrentPosition());
                telemetry.addData("Motor", "full rght-back:%02.1f, %d", rightBackPower, motorBackRight.getCurrentPosition());
                telemetry.addData("Motor", "full left-frnt:%02.1f, %d", leftFrontPower, motorFrontLeft.getCurrentPosition());
                telemetry.addData("Motor", "full rght-frnt:%02.1f, %d", rightFrontPower, motorFrontRight.getCurrentPosition());
                telemetry.addData("Motor", "SwitchFront ;%b", switchFront);
            }
        }
    }

            protected void sleep (long milliseconds){
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }


        }
    }
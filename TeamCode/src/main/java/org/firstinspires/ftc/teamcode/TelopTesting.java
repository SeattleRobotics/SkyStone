package org.firstinspires.ftc.teamcode;

//import com.acmerobotics.dashboard.FtcDashboard;
//import com.acmerobotics.dashboard.config.Config;
//import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
//import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
//import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.util.ElapsedTime;

//@Config
@TeleOp(name = "Jason and Aurick Teleop", group = "Jason and Aurick Teleop Testing")
public class TelopTesting extends OpMode {

    // Variables we want to play with in dashboard


    // Declare OpMode members.
    private ElapsedTime runtime = new ElapsedTime();

    // Chassis
    private DcMotor motorBackLeft = null;
    private DcMotor motorFrontLeft = null;
    private DcMotor motorBackRight = null;
    private DcMotor motorFrontRight = null;

    // Stone placement
    private DcMotor elevatorMotor = null;
    private DcMotor craneMotor = null;
    private Servo handServo = null;

    // Stone sucker
    private DcMotor leftSuckerMotor = null;
    private DcMotor rightSuckerMotor = null;

    // Crab
    private Servo crabServo = null;

    // State Variables
    private boolean g1startLastPos = false;
    private boolean g1xLastPos = false;
    private boolean g1lefttriggerLastPos = false;
    private boolean g2lefttriggerLastPos = false;

    private boolean driveReverse = false;

    private boolean elevatorAutomate = false;

    private boolean crabUp;

    // Constants
    private static final int ELEVATOR_LOW = 0;
    private static final int ELEVATOR_FUDGE = 100;
    private static final double ELEVATOR_UP_POWER = 1.0;                                             // Speed we want elevator to move at when being manually controlled for fine tuning height
    private static final double ELEVATOR_DOWN_POWER = .5;
    private static final double ELEVATOR_HOLD_POWER = .001;
    private static final int ELEVATOR_HIGH = 3200;

    private static final double CRANE_POWER = 1;                                                    // Speed we want the crane to move
    private static final int CRANE_FUDGE = 10;
    private static final int CRANE_OUT = 700;

    private static final double CRAB_DOWN = 0.4;
    private static final double CRAB_UP = 0.65;

    private static final double HAND_IN = 1.0;
    private static final double HAND_OUT = 0.0;

    @Override
    public void init() {
        //telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        telemetry.addData("Status", "Initializing");

        // Chassis
        motorBackLeft = hardwareMap.get(DcMotor.class, "motor0");
        motorBackRight = hardwareMap.get(DcMotor.class, "motor1");
        motorFrontLeft = hardwareMap.get(DcMotor.class, "motor2");
        motorFrontRight = hardwareMap.get(DcMotor.class, "motor3");


        motorBackLeft.setDirection(DcMotor.Direction.REVERSE);
        motorFrontLeft.setDirection(DcMotor.Direction.REVERSE);
        motorBackRight.setDirection(DcMotor.Direction.FORWARD);
        motorFrontRight.setDirection(DcMotor.Direction.FORWARD);

        //Elevator
        elevatorMotor = hardwareMap.get(DcMotor.class, "elevator");
        elevatorMotor.setDirection(DcMotor.Direction.REVERSE);
        elevatorMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        elevatorMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        elevatorMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Crane
        craneMotor = hardwareMap.get(DcMotor.class, "motorCrane");
        craneMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        craneMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        craneMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Hand
        handServo = hardwareMap.get(Servo.class, "servoGripper");
        handServo.setPosition(HAND_OUT);

        // Stone sucker
        leftSuckerMotor = hardwareMap.get(DcMotor.class, "leftSucc");
        leftSuckerMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightSuckerMotor = hardwareMap.get(DcMotor.class, "rightSucc");
        rightSuckerMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightSuckerMotor.setDirection(DcMotor.Direction.REVERSE);

        // Crab
        crabServo = hardwareMap.get(Servo.class, "servoCrab");
        crabServo.setPosition(CRAB_UP);
        crabUp = true;

        // Tell the driver that initialization is complete.
        telemetry.addData("Status", "Initialized");
    }

    @Override
    public void init_loop() {
    }

    @Override
    public void start() {
        runtime.reset();
    }

    @Override
    public void loop() {

        /************************
         // **** UPDATE STATE VARIABLES
         ************************/
        int elevatorPosition = elevatorMotor.getCurrentPosition();                                 // Get current position and flip from negative to positive to make logic more readable
        int cranePosition = craneMotor.getCurrentPosition();
        double elevatorPower = 0.0;                                                                 // Initialize to zero as the default power level and then determine below if we need to move
        double cranePower = 0.0;

        // Toggle front of robot
        if (gamepad1.start != g1startLastPos) {
            g1startLastPos = !g1startLastPos;
            if (gamepad1.start)
                driveReverse = !driveReverse;
        }


        boolean g1lefttriggerPressed = (gamepad1.left_trigger > 0.5);
        if (g1lefttriggerPressed != g1lefttriggerLastPos) {
            g1lefttriggerLastPos = !g1lefttriggerLastPos;
            if (g1lefttriggerPressed) {
                handServo.setPosition(HAND_OUT);
                elevatorAutomate = true;
            }
        }

        boolean g2lefttriggerPressed = (gamepad2.left_trigger > 0.5);
        if (g2lefttriggerPressed != g2lefttriggerLastPos) {
            g2lefttriggerLastPos = !g2lefttriggerLastPos;
            if (g2lefttriggerPressed) {
                handServo.setPosition(HAND_OUT);
                elevatorAutomate = true;
            }
        }

        if ((gamepad1.y || gamepad1.a) || (gamepad1.dpad_up || gamepad1.dpad_down) ||               // If there is any manual input cancel the automation
                (gamepad1.dpad_right || gamepad1.dpad_left)) {
            elevatorAutomate = false;
        }

        /************************
         **** AUTOMATION
         ************************/
        if (elevatorAutomate) {
            boolean elevatorPhase1 = (elevatorPosition <= 1000) || (elevatorPosition >= 700);       // We need the elevator above 700 to be able to pull the crane all the way in otherwise it will hit the crab
            boolean elevatorPhase2 = (elevatorPosition <= 50);
            boolean craneDone = (cranePosition <= 0);

            if (!craneDone)
                cranePower = -CRANE_POWER;
            else
                cranePower = 0;

            if (!elevatorPhase1) {
                if (elevatorPosition < 700)
                    elevatorPower = ELEVATOR_UP_POWER;                                              // if the elevator is too low move it up
                else
                    elevatorPower = -ELEVATOR_DOWN_POWER;                                           // otherwise it is too high so begin moving it down
            }
            else if (elevatorPhase1 && !craneDone)                                                  // if the first phase of the elevator is done but crane isn't stop the elevator until the crane finishes
                elevatorPower = 0;
            else if (elevatorPhase1 & craneDone)                                                    // Once phase1 of elevator is done and crane is done move elevator the rest of the way down
                elevatorPower = -ELEVATOR_DOWN_POWER;

            if (elevatorPhase2)                                                                     // Once the phase2 of the elevator is done the automation is done
                elevatorAutomate = false;
        }

        /************************
         // **** DRIVE SYSTEM
         ************************/
        double driveForwardBack = gamepad1.left_stick_y;
        double driveStrafe = gamepad1.left_stick_x;
        double turn = -gamepad1.right_stick_x;

        // If in reverse mode flip the controller input for direction and strafe
        if (driveReverse) {
            driveForwardBack *= -1;
            driveStrafe *= -1;
        }

        double cap = 1.0f;
        double powerReductionFactor = 4.0f;
        double leftRearPower = Range.clip(driveForwardBack + turn + (driveStrafe), -cap, cap);
        double rightRearPower = Range.clip(driveForwardBack - turn - (driveStrafe), -cap, cap);
        double leftFrontPower = Range.clip(driveForwardBack + turn - driveStrafe, -cap, cap);
        double rightFrontPower = Range.clip(driveForwardBack - turn + driveStrafe, -cap, cap);

        // If right stick button is depressed move robot at quarter speed
        if (gamepad1.right_stick_button) {
            leftRearPower = leftRearPower / powerReductionFactor;
            rightRearPower = rightRearPower / powerReductionFactor;
            leftFrontPower = leftFrontPower / powerReductionFactor;
            rightFrontPower = rightFrontPower / powerReductionFactor;
        }

        // Set motor powers
        motorBackLeft.setPower(leftRearPower);
        motorBackRight.setPower(rightRearPower);
        motorFrontLeft.setPower(leftFrontPower);
        motorFrontRight.setPower(rightFrontPower);

        /************************
         // **** SUCKER *********
         ************************/
        double suckerPower = 0.0;
        if ((!gamepad1.right_bumper) && (gamepad1.right_trigger == 0)) {                            // If no input from controller 1 then look at controller 2
            // ---CONTROLLER 2
            suckerPower = gamepad2.right_trigger;
            if (gamepad2.right_bumper)
                suckerPower = -1;
        } else {
            suckerPower = gamepad1.right_trigger;
            if (gamepad1.right_bumper) {
                suckerPower = -1;
            }
        }

        rightSuckerMotor.setPower(suckerPower);
        leftSuckerMotor.setPower(suckerPower);

        /************************
         **** ELEVATOR
         ************************/
        if ((!gamepad1.y) && (!gamepad1.a)) {                                                       // If no input from controller 1 then look at controller 2
            if (gamepad2.y)                                                                            // Operator 1's Y is pressed and we haven't reached our maximum height
                elevatorPower = ELEVATOR_UP_POWER;                                                     // Move up at a controllable speed
            else if ((gamepad2.a) && (elevatorPosition - ELEVATOR_FUDGE >= ELEVATOR_LOW))                   // Operator 1's A is pressed and we haven't reached the bottom
                elevatorPower = -ELEVATOR_DOWN_POWER;                                                  // Move down at a controllable speed
        } else {
            if (gamepad1.y)                                                                            // Operator 1's Y is pressed and we haven't reached our maximum height
                elevatorPower = ELEVATOR_UP_POWER;                                                     // Move up at a controllable speed
            else if ((gamepad1.a) && (elevatorPosition - ELEVATOR_FUDGE >= ELEVATOR_LOW))                   // Operator 1's A is pressed and we haven't reached the bottom
                elevatorPower = -ELEVATOR_DOWN_POWER;                                                  // Move down at a controllable speed
        }

        if (elevatorPosition > 200 && elevatorPower == 0)                                           // If the elevator is up and power is 0 we need to change it to a small up to make the motor hold
            elevatorPower = ELEVATOR_HOLD_POWER;

        elevatorMotor.setPower(elevatorPower);                                                      // Set direction and power determined above to the motor

        /************************
         **** CRANE
         ************************/
        if ((!gamepad1.dpad_down) && (!gamepad1.dpad_up)) {                                                       // If no input from controller 1 then look at controller 2
            if (gamepad2.dpad_down)                                                                   // Ignore input if crane is already all the way back
                cranePower = -CRANE_POWER;                                                       // Operator 1 is moving crane so move at slower speed for small adjustments

            if (gamepad2.dpad_up && cranePosition + CRANE_FUDGE < CRANE_OUT)                           // Ignore input if crane is already all the way out
                cranePower = CRANE_POWER;                                                        // Operator 1 is moving crane so move at slower speed for small adjustments
        } else {
            if (gamepad1.dpad_down)                                                                   // Ignore input if crane is already all the way back
                cranePower = -CRANE_POWER;                                                       // Operator 1 is moving crane so move at slower speed for small adjustments

            if (gamepad1.dpad_up && cranePosition + CRANE_FUDGE < CRANE_OUT)                           // Ignore input if crane is already all the way out
                cranePower = CRANE_POWER;                                                        // Operator 1 is moving crane so move at slower speed for small adjustments
        }

        craneMotor.setPower(cranePower);                                                            // Set power determined above to motor

        /************************
         **** HAND
         ************************/
        if ((!gamepad1.dpad_left) && (!gamepad1.dpad_right)) {                                                       // If no input from controller 1 then look at controller 2
            if (gamepad2.dpad_left)
                handServo.setPosition(HAND_OUT);
            if (gamepad2.dpad_right)
                handServo.setPosition(HAND_IN);
        } else {
            if (gamepad1.dpad_left)
                handServo.setPosition(HAND_OUT);
            if (gamepad1.dpad_right)
                handServo.setPosition(HAND_IN);
        }

        /************************
         **** CRAB
         ************************/
        if (gamepad1.x != g1xLastPos) {
            g1xLastPos = !g1xLastPos;
            if (gamepad1.x)
                if (crabUp) {
                    crabServo.setPosition(CRAB_DOWN);
                    crabUp = false;
                } else {
                    crabServo.setPosition(CRAB_UP);
                    crabUp = true;
                }
        }

        /************************
         **** TELEMETRY
         ************************/
        telemetry.addData("Status", runtime.toString());
//        telemetry.addData("Front Motors", "Left (%.2f), Right (%.2f)", leftFrontPower, rightFrontPower);
//        telemetry.addData("Rear Motors", "Left (%.2f), Right (%.2f)", leftRearPower, rightRearPower);
        telemetry.addData("Drive Reverse ", driveReverse);
        telemetry.addData("Elevator Height", elevatorPosition);
        telemetry.addData("Elevator Power", elevatorPower);
        telemetry.addData("Crane Position", cranePosition);
    }

    @Override
    public void stop() {
    }
}



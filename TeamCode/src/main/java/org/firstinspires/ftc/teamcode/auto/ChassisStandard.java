package org.firstinspires.ftc.teamcode.auto;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;

import java.util.List;
import com.qualcomm.robotcore.hardware.DigitalChannel;

import static java.lang.Math.abs;

public abstract class ChassisStandard extends OpMode {

    enum InitStage {
        INIT_STAGE_START,
        INIT_STAGE_ARM,
        INIT_STAGE_VUFORIA,
        INIT_STAGE_FINISHED
    }

    private static final int NUDGE_TIME = 1;
    private static final float NUDGE_ANGLE = 4.0f;
    private static final float NORMALIZE_ANGLE = 360.0f;

    // vision detection variables/state
    private final int SCREEN_WIDTH = 600;
    public static final String UNKNOWN = "unknown";
    private static final String TFOD_MODEL_ASSET = "Skystone.tflite";
    private static final String LABEL_FIRST_ELEMENT = "Stone";
    private static final String LABEL_SECOND_ELEMENT = "Skystone";
    private static final String VUFORIA_KEY =
            "AfgOBrf/////AAABmRjMx12ilksPnWUyiHDtfRE42LuceBSFlCTIKmmNqCn2EOk3I4NtDCSr0wCLFxWPoLR2qHKraX49ofQ2JknI76SJS5Hy8cLbIN+1GlFDqC8ilhuf/Y1yDzKN6a4n0fYWcEPlzHRc8C1V+D8vZ9QjoF3r//FDDtm+M3qlmwA7J/jNy4nMSXWHPCn2IUASoNqybTi/CEpVQ+jEBOBjtqxNgb1CEdkFJrYGowUZRP0z90+Sew2cp1DJePT4YrAnhhMBOSCURgcyW3q6Pl10XTjwB4/VTjF7TOwboQ5VbUq0wO3teE2TXQAI53dF3ZUle2STjRH0Rk8H94VtHm9u4uitopFR7zmxVl3kQB565EUHwfvG";

    // is sound playing?
    boolean soundPlaying = false;
    int bruhSoundID = -1;

    protected ChassisConfig config;
    protected boolean madeTheRun = false;
    protected ElapsedTime runtime = new ElapsedTime();
    private InitStage initStage = InitStage.INIT_STAGE_START;

    // vuforia/stone detection stuff
    protected String stoneconfig;
    protected VuforiaLocalizer vuforia;
    protected TFObjectDetector tfod;
    private int lastStones = 0;
    private int lastSkyStones = 0;
    private float leftEdgeSkyStone = -1;
    private float rightEdgeSkyStone = -1;
    private float widthSkyStone = -1;
    private List<Recognition> lastRecognitions = null;

    // Motors
    private DcMotor motorBackLeft;
    private DcMotor motorBackRight;
    private DcMotor motorFrontLeft;
    private DcMotor motorFrontRight;
    private boolean reverseMotors = false;
    private String motorTurnType = "none";
    private float motorTurnDestination = 0.0f;
    private float motorTurnAngleToGo = 0.0f;
    private float motorTurnAngleAdjustedToGo = 0.0f;
    private PIDController motorPid = new PIDController(.05, 0, 0);

    // Crab
    protected Servo crab;
    private double crabAngle;

    //Arm
    private DcMotor crane;
    //private DcMotor extender;
    private Servo hand;

    //Succ
    private DcMotor leftSucc;
    private DcMotor rightSucc;


    // Fingers
    protected Servo fingerLeft;
    protected Servo fingerRight;
    private double fingerLeftAngle;
    private double fingerRightAngle;

    // Elevator
    private DcMotor elevator;
    private double angleAnkle;
    private DigitalChannel elevatorMagnet;

    // Gyroscope
    private BNO055IMU bosch;

    // Optionally turn on/off the subsystems.
    protected boolean useGyroScope = true;
    protected boolean useMotors = true;
    protected boolean useTimeouts = true;
    protected boolean useCrab = true;
    protected boolean useElevator = true;
    protected boolean useFingers = true;
    protected boolean useVuforia = false;
    protected boolean useMagnets = true;
    protected boolean useArm = true;


    protected ChassisStandard() {
        this(ChassisConfig.forDefaultConfig());
    }

    protected ChassisStandard(ChassisConfig config) {
        this.config = config;
        stoneconfig = UNKNOWN;
    }

     /*
        Robot Controller Callbacks
     */

    @Override
    public void start() {
        // Reset the game timer.
        runtime.reset();
    }

    @Override
    public void stop() {
    }

    @Override
    public void init() {
        initMotors();
        initTimeouts();
        initGyroscope();
        initMagnets();
        initStage = InitStage.INIT_STAGE_ARM;
    }

    /**
     * Code to run REPEATEDLY after the driver hits INIT, but before they hit PLAY
     */
    @Override
    public void init_loop() {
        switch (initStage) {
            case INIT_STAGE_ARM:
                initCrab();
                initFingers();
                initStage = InitStage.INIT_STAGE_VUFORIA;
                break;

            case INIT_STAGE_VUFORIA:
                initVuforia();
                initStage = InitStage.INIT_STAGE_FINISHED;
                break;

            case INIT_STAGE_FINISHED:
            default:
                break;
        }

        printStatus();
    }

    @Override
    public void loop () {

        if (madeTheRun == false) {
           makeTheRun();
           madeTheRun = true;
        }

        printStatus();
    }

    public void makeTheRun () {
        // Override this in a base class!
    }


    protected void switchMotorDirection() {
        this.reverseMotors = !this.reverseMotors;
    }

    /**
     *
     */
    protected void printStatus() {

        if (initStage == InitStage.INIT_STAGE_FINISHED) {
            if (useGyroScope) {
                telemetry.addData("Gyro", "angle: " + this.getGyroscopeAngle());
            } else {
                telemetry.addData("Gyro", "DISABLED");
            }

            if (useMotors) {
                telemetry.addData("Motor: frnt", "left:%02.1f, (%d), rigt: %02.1f, (%d)",
                        motorFrontLeft.getPower(), motorFrontLeft.getCurrentPosition(), motorFrontRight.getPower(), motorFrontRight.getCurrentPosition());
                telemetry.addData("Motor: back", "left:%02.1f, (%d), rigt: %02.1f, (%d)",
                        motorBackLeft.getPower(), motorBackLeft.getCurrentPosition(), motorBackRight.getPower(), motorBackRight.getCurrentPosition());
                telemetry.addData("Motor: turn", "type=%s, now: %02.1f, dest: %02.1f, togo= %02.1f, togo2= %02.1f",
                        motorTurnType, this.getGyroscopeAngle(), motorTurnDestination, motorTurnAngleToGo, motorTurnAngleAdjustedToGo);
            } else {
                telemetry.addData("Motor", "DISABLED");
            }

            if (useCrab) {
                telemetry.addData("Crab", "%02.1f (%02.1f)", crab.getPosition(), crabAngle);
            } else {
                telemetry.addData("Crab", "DISABLED");
            }

            if (useFingers) {
                telemetry.addData("Finger", "%02.1f (%02.1f), %02.1f (%02.1f)",
                       /* fingerLeft.getPosition()*/ -1.0f, fingerLeftAngle, /*fingerRight.getPosition() */ -1.0f, fingerRightAngle);
            } else {
                telemetry.addData("Finger", "DISABLED");
            }


            if (useVuforia) {
                int numStones = 0;
                int numSkyStones = 0;

                if (tfod != null) {
                    // getUpdatedRecognitions() will return null if no new information is available since
                    // the last time that call was made.
                    List<Recognition> updatedRecognitions = getRecognitions();
                    if (updatedRecognitions == null) {
                        numStones = lastStones;
                        numSkyStones = lastSkyStones;
                    } else {
                        numStones = updatedRecognitions.size();
                        for (Recognition recognition : updatedRecognitions) {
                            if (recognition.getLabel() == "Skystone") {
                                numSkyStones++;
                                numStones--;
                            }
                        }
                        lastStones = numStones;
                        lastSkyStones = numSkyStones;
                    }
                }

                telemetry.addData("StoneDetect", "mode:%s, norm: %d, sky: %d, loc: %02.1f,  %02.1f,  %02.1f", stoneconfig, numStones, numSkyStones,
                        leftEdgeSkyStone, rightEdgeSkyStone, widthSkyStone);
            } else {
                telemetry.addData("StoneDetect", "DISABLED");
            }

            telemetry.addData("Status", "madeRun=%b, time: %s", madeTheRun, runtime.toString());
        } else {
            telemetry.addData("Status", "still initializing... %s", initStage);
        }

        telemetry.update();
    }


    /*
        MOTOR SUBSYTEM
     */

    protected void initMotors() {

        // Initialize the motors.
        if (useMotors) {
            try {
                motorBackLeft = hardwareMap.get(DcMotor.class, "motorBackLeft");
                motorBackRight = hardwareMap.get(DcMotor.class, "motorBackRight");

                // Most robots need the motor on one side to be reversed to drive forward
                // Reverse the motor that runs backwards when connected directly to the battery
                motorBackLeft.setDirection(config.isLeftMotorReversed() ? DcMotor.Direction.REVERSE : DcMotor.Direction.FORWARD);
                motorBackRight.setDirection(config.isRightMotorReversed() ? DcMotor.Direction.REVERSE : DcMotor.Direction.FORWARD);

                // initialize the encoder
                motorBackLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                motorBackRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                motorBackLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                motorBackRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                motorBackLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
                motorBackRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
                //motorBackLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
                //motorBackRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

                if (config.getUseFourWheelDrive()) {
                    motorFrontLeft = hardwareMap.get(DcMotor.class, "motorFrontLeft");
                    motorFrontRight = hardwareMap.get(DcMotor.class, "motorFrontRight");

                    motorFrontLeft.setDirection(config.isLeftMotorReversed() ? DcMotor.Direction.REVERSE : DcMotor.Direction.FORWARD);
                    motorFrontRight.setDirection(config.isRightMotorReversed() ? DcMotor.Direction.REVERSE : DcMotor.Direction.FORWARD);

                    motorFrontLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    motorFrontRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    motorFrontLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                    motorFrontRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                    motorFrontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
                    motorFrontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
                   // motorFrontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
                   // motorFrontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
                }
            } catch (Exception e) {
                telemetry.addData("motors", "exception on init: " + e.toString());
                useMotors = false;
            }
        }
    }

    protected void initCrab() {
        if (useCrab) {
            try {
                crab = hardwareMap.get(Servo.class, "crab");
            } catch (Exception e) {
                telemetry.addData("crab", "exception on init: " + e.toString());
                useCrab = false;
            }
            raiseCrab();
        }
    }

    /*
    if (useArm) {
        try {
            crane = hardwareMap.get(DcMotor.class, "motorCrane");
            hand = hardwareMap.get(Servo.class, "servoGripper");
            //  extender = hardwareMap.get(DcMotor.class, "motorExtend");

        } catch (Exception e) {
            telemetry.addData("Arm", "exception on init: " + e.toString());
            crane = null;
            hand = null;
            //extender = null;
        }
        if (crane == null) {
            telemetry.addData("Arm", "You forgot to set up crane, set up Crane");
            useArm = false;
        }
        if (hand == null) {
            telemetry.addData("Arm", "You forgot to set up hand, set up the hand");
            useArm = false;
        }
            /*if (extender == null) {
                telemetry.addData("Arm", "You forgot to set up extender, set up the extender");
                useArm = false;
            }
    }


            if (useElevator) {
        try {
            elevator = hardwareMap.get(DcMotor.class, "elevator");
        } catch (Exception e) {
            telemetry.addData("elevator", "exception on init: " + e.toString());
            useElevator = false;
        }
    }

            if (useSucc) {
        try {
            leftSucc = hardwareMap.get(DcMotor.class, "leftSucc");
            rightSucc = hardwareMap.get(DcMotor.class, "rightSucc");

        } catch (Exception e) {
            telemetry.addData("Succ", "exception on init: " + e.toString());
            useElevator = false;
        }
    }

    protected void initElevator() {
        if (useElevator) {
            try {
                elevator = hardwareMap.get(DcMotor.class, "elevator");
            } catch (Exception e) {
                telemetry.addData("elevator", "exception on init: " + e.toString());
                useElevator = false;
            }
        }
    }*/

    protected void initFingers() {
        if (useFingers) {
            try {
                fingerLeft = hardwareMap.get(Servo.class, "servoLeftFinger");

                fingerRight = hardwareMap.get(Servo.class, "servoRightFinger");

            } catch (Exception e) {
                telemetry.addData("finger", "exception on init: " + e.toString());
                sleep(2000);
                useFingers = false;
            }

            raiseRightFinger();
            raiseLeftFinger();
        }
    }


    protected void initTimeouts() {
        // This code prevents the OpMode from freaking out if you go to sleep for more than a second.
        if (useTimeouts) {
            this.msStuckDetectInit = 30000;
            this.msStuckDetectInitLoop = 30000;
            this.msStuckDetectStart = 30000;
            this.msStuckDetectLoop = 30000;
            this.msStuckDetectStop = 30000;
        }
    }


    protected boolean initGyroscope() {
        if (useGyroScope) {
            bosch = hardwareMap.get(BNO055IMU.class, "imu0");
            telemetry.addData("Gyro", "class:" + bosch.getClass().getName());

            BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
            parameters.mode = BNO055IMU.SensorMode.IMU;
            parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
            parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
            parameters.loggingEnabled = false;
            parameters.loggingTag = "bosch";
            //parameters.calibrationDataFile = "MonsieurMallahCalibration.json"; // see the calibration sample opmode
            boolean boschInit = bosch.initialize(parameters);
            return boschInit;
        } else {
            return true;
        }
    }

    protected boolean initMagnets() {
        if (useMagnets) {
            elevatorMagnet = hardwareMap.get(DigitalChannel.class, "elevatorMagnet");
            telemetry.addData("Magnet", "class:" + elevatorMagnet.getClass().getName());
            return true;


        } else {
            useMagnets = false;
            return false;

        }
    }

    protected boolean isElevatorMagnetOn() {
        return !elevatorMagnet.getState();
    }


    /* VUFORIA */

    /**
     * Initialize the Vuforia localization engine.
     */
    private void initVuforia() {

        if (useVuforia) {

            lastStones = 0;
            lastSkyStones = 0;

            try {
                /*
                 * Configure Vuforia by creating a Parameter object, and passing it to the Vuforia engine.
                 */
                VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();
                parameters.vuforiaLicenseKey = VUFORIA_KEY;
                parameters.cameraName = hardwareMap.get(WebcamName.class, "Webcam 1");

                telemetry.addData("Vuforia 1", "found camera");
                telemetry.update();
                sleep(2000);

                //  Instantiate the Vuforia engine
                vuforia = ClassFactory.getInstance().createVuforia(parameters);

                telemetry.addData("Vuforia 1", "found vuforia");
                telemetry.update();
                sleep(2000);

                // Loading trackables is not necessary for the TensorFlow Object Detection engine.
                if (ClassFactory.getInstance().canCreateTFObjectDetector()) {
                    initTfod();
                } else {
                    telemetry.addData("Sorry!", "This device is not compatible with TFOD");
                }
                sleep(1000);

                telemetry.addData("Vuforia 1", "init tfod");
                telemetry.update();
                sleep(1000);

                if (tfod != null) {
                    tfod.activate();
                }

                telemetry.addData("Vuforia 1", "activate");
                telemetry.update();
                sleep(1000);

            } catch (Exception e) {
                telemetry.addData("vuforia", "exception on init: " + e.toString());
                useVuforia = false;
            }

            telemetry.addData("StoneDetectLoc", "loc=%s", stoneconfig);
            printStatus();
            sleep(5000);
        }
    }

    /**
     * Initialize the TensorFlow Object Detection engine.
     */
    private void initTfod() {
        int tfodMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "tfodMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        TFObjectDetector.Parameters tfodParameters = new TFObjectDetector.Parameters(tfodMonitorViewId);
        tfodParameters.minimumConfidence = 0.8;

        tfod = ClassFactory.getInstance().createTFObjectDetector(tfodParameters, vuforia);
        tfod.loadModelFromAsset(TFOD_MODEL_ASSET, LABEL_FIRST_ELEMENT, LABEL_SECOND_ELEMENT);
    }


    protected List<Recognition> getRecognitions() {
        // getUpdatedRecognitions() will return null if no new information is available since
        // the last time that call was made.
        List<Recognition> updatedRecognitions = tfod.getUpdatedRecognitions();
        if (updatedRecognitions == null) {
            updatedRecognitions = lastRecognitions;
        }
        if (updatedRecognitions != null) {
            lastRecognitions = updatedRecognitions;
        }
        return updatedRecognitions;
    }

    protected void scanStones() {
        if (tfod != null) {

            // step through the list of recognitions and display boundary info.
            int i = 0;
            for (Recognition recognition : getRecognitions()) {
                if (recognition.getLabel() == "Skystone") {
                    leftEdgeSkyStone = recognition.getLeft();
                    rightEdgeSkyStone = recognition.getRight();
                    widthSkyStone = rightEdgeSkyStone - leftEdgeSkyStone;

                   /* telemetry.addData(String.format("StoneDetect label (%d)", i), recognition.getLabel());
                    telemetry.addData(String.format("StoneDetect left,top (%d)", i), "%.03f , %.03f",
                            recognition.getLeft(), recognition.getTop());
                    telemetry.addData(String.format("StoneDetect right,bottom (%d)", i), "%.03f , %.03f",
                            recognition.getRight(), recognition.getBottom());
                    telemetry.update(); */
                }
            }

            /*int leftBorder = 140;
            int rightBorder = 315;
            if (leftEdgeSkyStone < leftBorder) {
                stoneconfig = "LEFT";
            } else if (leftEdgeSkyStone > rightBorder) {
                stoneconfig = "RIGHT";
            } else {
                stoneconfig = "CENTER";
            } */

            int leftBorder = 140;
            int rightBorder = 315;

            if (widthSkyStone < 300) {
                // skystone is normal sized - only one stone, we can trust it.
                if (leftEdgeSkyStone < 50) {
                    stoneconfig = "LEFT";
                } else if (leftEdgeSkyStone > 350) {
                    stoneconfig = "RIGHT";
                } else {
                    stoneconfig = "CENTER";
                }
            } else {
                if (rightEdgeSkyStone > 550) {
                    stoneconfig = "RIGHT";
                } else if (rightEdgeSkyStone > 450) {
                    stoneconfig = "CENTER";
                } else {
                    stoneconfig = "LEFT";
                }
            }
        }
    }


    public void dropLeftFinger() {
        if (useFingers) {
            fingerLeftAngle = 1.0;
            fingerLeft.setPosition(fingerLeftAngle);
        }
    }

    public void raiseLeftFinger() {
        if (useFingers) {
            fingerLeftAngle = 0.0;
            fingerLeft.setPosition(fingerLeftAngle);
        }
    }
    
    public void dropRightFinger() {
        if (useFingers) {
            fingerRightAngle = 0.3;
            fingerRight.setPosition(fingerRightAngle);
        }
    }

    public void raiseRightFinger() {
        if (useFingers) {
            fingerRightAngle = 1.0;
            fingerRight.setPosition(fingerRightAngle);
        }
    }

    public void dropCrab() {
        if (useCrab) {
            crabAngle = 0.4;
            crab.setPosition(crabAngle);
        }
    }

    public void raiseCrab() {
        if (useCrab) {
            crabAngle = 1.0;
            crab.setPosition(crabAngle);
        }
    }

    public void raiseElevator() {
        if(useElevator) {
            angleAnkle = 1.0;
            elevator.setPower(angleAnkle);
        }
    }

    public void dropElevator() {
        if(useElevator) {
            angleAnkle = 0.0;
            elevator.setPower(angleAnkle);
        }
    }


    float getAngleDifference(float from, float to)
    {
        float difference = to - from;
        while (difference < -180) difference += 360;
        while (difference > 180) difference -= 360;
        return difference;
    }



    protected void encoderDrive(double inches) {
        encoderDrive(inches, inches);
    }

    protected void encoderDrive(double leftInches, double rightInches) {
        double speed = config.getMoveSpeed();
        encoderDrive(leftInches, rightInches, speed);
    }

    protected void encoderDrive(double leftInches, double rightInches, double speed) {
        encoderDrive(leftInches, rightInches, speed, -1);
    }

    protected void encoderDrive(double leftInches, double rightInches, double speed, float desiredAngle) {
        float startAngle = getGyroscopeAngle();

        // Jump out if the motors are turned off.
        if (!useMotors)
            return;

        if (reverseMotors) {
            leftInches = -leftInches;
            rightInches = -rightInches;
        }

        if (desiredAngle >= 0.0) {
            motorPid.reset();
            motorPid.setSetpoint(0);
            motorPid.setOutputRange(0, speed);
            motorPid.setInputRange(-90, 90);
            motorPid.enable();
        }

        double countsPerInch = config.getRearWheelSpeed() / (config.getRearWheelDiameter() * Math.PI);

        // Get the current position.
        int leftBackStart = motorBackLeft.getCurrentPosition();
        int rightBackStart = motorBackRight.getCurrentPosition();
        int leftFrontStart = 0;
        int rightFrontStart = 0;
        if (config.getUseFourWheelDrive()) {
            leftFrontStart = motorFrontLeft.getCurrentPosition();
            rightFrontStart = motorFrontRight.getCurrentPosition();
        }
        telemetry.addData("encoderDrive", "Starting %7d, %7d, %7d, %7d",
                leftBackStart, rightBackStart, leftFrontStart, rightFrontStart);

        // Determine new target position, and pass to motor controller
        int leftBackTarget = leftBackStart + (int) (leftInches * countsPerInch);
        int rightBackTarget = rightBackStart + (int) (rightInches * countsPerInch);
        int leftFrontTarget = 0;
        int rightFrontTarget = 0;
        if (config.getUseFourWheelDrive()) {
            leftFrontTarget = leftFrontStart + (int) (leftInches * countsPerInch);
            rightFrontTarget = rightFrontStart + (int) (rightInches * countsPerInch);
        }

        motorBackLeft.setTargetPosition(leftBackTarget);
        motorBackRight.setTargetPosition(rightBackTarget);
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setTargetPosition(leftFrontTarget);
            motorFrontRight.setTargetPosition(rightFrontTarget);
        }
        telemetry.addData("encoderDrive", "Target %7d, %7d, %7d, %7d",
                leftBackTarget, rightBackTarget, leftFrontTarget, rightFrontTarget);

        // Throttle speed down as we approach our target
        if ((abs(leftInches) < 8.0) || (abs(rightInches) < 8.0)) {
            speed *= 0.5;
        } else  if ((abs(leftInches) < 5.0) || (abs(rightInches) < 5.0)) {
            speed *= 0.25;
        }

        // Turn On RUN_TO_POSITION
        motorBackLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorBackRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motorBackLeft.setPower(abs(speed));
        motorBackRight.setPower(abs(speed));
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            motorFrontRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            motorFrontLeft.setPower(abs(speed));
            motorFrontRight.setPower(abs(speed));
        }

        // keep looping while we are still active, and there is time left, and both motors are running.
        // Note: We use (isBusy() && isBusy()) in the loop test, which means that when EITHER motor hits
        // its target position, the motion will stop.  This is "safer" in the event that the robot will
        // always end the motion as soon as possible.
        // However, if you require that BOTH motors have finished their moves before the robot continues
        // onto the next step, use (isBusy() || isBusy()) in the loop test.
        ElapsedTime motorOnTime = new ElapsedTime();
        boolean keepGoing = true;
        while (keepGoing && (motorOnTime.seconds() < 30)) {

            if (config.getUseFourWheelDrive()) {
                telemetry.addData("encoderDrive1", "Running at %7d, %7d, %7d, %7d",
                        motorBackLeft.getCurrentPosition(),
                        motorBackRight.getCurrentPosition(),
                        motorFrontLeft.getCurrentPosition(),
                        motorFrontRight.getCurrentPosition());
                telemetry.addData("encoderDrive2", "Running to %7d, %7d, %7d, %7d",
                        leftBackTarget,
                        rightBackTarget,
                        leftFrontTarget,
                        rightFrontTarget);
                keepGoing = motorBackRight.isBusy() && motorBackLeft.isBusy() && motorFrontLeft.isBusy() && motorFrontRight.isBusy();
            } else {
                telemetry.addData("encoderDrive1", "Running at %7d, %7d",
                        motorBackLeft.getCurrentPosition(),
                        motorBackRight.getCurrentPosition());
                telemetry.addData("encoderDrive2", "Running to %7d, %7d",
                        leftBackTarget,
                        rightBackTarget);
                keepGoing = motorBackRight.isBusy() && motorBackLeft.isBusy();
            }

            if (keepGoing) {
                // Calculate PID correction = straightne out the line!
                double correction = 0;
                if (desiredAngle >= 0.0f) {
                    float currentAngle = getGyroscopeAngle();
                    float angleOffset = getAngleDifference(currentAngle, desiredAngle);
                    correction = motorPid.performPID(angleOffset);
                    if ((leftInches < 0) && (rightInches < 0)) {
                        correction = -correction;
                    }
                }

                motorBackLeft.setPower(Math.abs(speed - correction));
                motorBackRight.setPower(Math.abs(speed + correction));
                if (config.getUseFourWheelDrive()) {
                    motorFrontLeft.setPower(Math.abs(speed - correction));
                    motorFrontRight.setPower(Math.abs(speed + correction));
                }
            }

           // telemetry.update();
            //sleep(100);
        }

        // Turn off RUN_TO_POSITION
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
        motorBackLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setPower(0);
            motorFrontRight.setPower(0);
            motorFrontLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            motorFrontRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }

        // Final turn.
        if (desiredAngle >= 0.0f) {
            float currentAngle = getGyroscopeAngle();
            float angleOffset = getAngleDifference(currentAngle, desiredAngle);
            if (angleOffset < 0.0f) {
                turnLeftAbsolute(desiredAngle);
            } else if (angleOffset > 0.0f) {
                turnRightAbsolute(desiredAngle);
            }
        }

        /*telemetry.addData("encoderDrive", "Finished (%s) at %7d,%7d,%7d,%7d to [%7d,%7d,%7d,%7d] (%7d,%7d,%7d,%7d)",
                motorOnTime.toString(),
                leftBackStart,
                rightBackStart,
                leftFrontStart,
                rightFrontStart,
                motorBackLeft.getCurrentPosition(),
                motorBackRight.getCurrentPosition(),
                motorFrontLeft.getCurrentPosition(),
                motorFrontRight.getCurrentPosition(),
                leftBackTarget,
                rightBackTarget,
                leftFrontTarget,
                rightFrontTarget); */
       // sleep(100);
    }


    protected void sleep(long milliseconds) {
        try {
            ElapsedTime sleepTime = new ElapsedTime();
            while (sleepTime.milliseconds() < milliseconds) {
                Thread.sleep(1);
                printStatus();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Always returns a number from 0-359.9999
    protected float getGyroscopeAngle() {
        if (useGyroScope) {
            Orientation exangles = bosch.getAngularOrientation(AxesReference.EXTRINSIC, AxesOrder.XYZ, AngleUnit.DEGREES);
            float gyroAngle = exangles.thirdAngle;
            //exangles.
            //telemetry.addData("angle", "angle: " + exangles.thirdAngle);
            float calculated = CrazyAngle.normalizeAngle(CrazyAngle.reverseAngle(gyroAngle));
            //telemetry.addData("angle2","calculated:" + calculated);
            return calculated;
        } else {
            return 0.0f;
        }
    }




     /*   protected void turnLeftAbsolute (float destinationAngle){
            boolean doesItWrapAtAll = (destinationAngle < 0.0);
            destinationAngle = CrazyAngle.normalizeAngle(destinationAngle);
            float currentAngle = getGyroscopeAngle();

            // Get it past the zero mark.
            if (doesItWrapAtAll) {
                boolean keepGoing = true;
                while (keepGoing) {
                    float oldAngle = currentAngle;
                    nudgeLeft();
                    currentAngle = getGyroscopeAngle();

                    float justMoved = oldAngle - currentAngle;
                    float stillNeed = currentAngle;
                    telemetry.addData("turnLeft1", "current=%.0f, old=%.0f, dst=%.0f, moved=%.0f, need=%.0f", currentAngle, oldAngle, destinationAngle, justMoved, stillNeed);
                    telemetry.update();

                    keepGoing = (justMoved > -50.0);
                }
            }

            // turn the last part
            while ((currentAngle - destinationAngle) > NUDGE_ANGLE) {

                float oldAngle = currentAngle;
                nudgeLeft();
                currentAngle = getGyroscopeAngle();

                float justMoved = oldAngle - currentAngle;
                float stillNeed = currentAngle - destinationAngle;
                telemetry.addData("turnLeft2", "current = %.0f, destination = %.0f, moved=%.0f, need=%.0f", currentAngle, destinationAngle, justMoved, stillNeed);
                telemetry.update();
            }

            // turn off motor.
            motorBackLeft.setPower(0);
            motorBackRight.setPower(0);
            if (config.getUseFourWheelDrive()) {
                motorFrontLeft.setPower(0);
                motorFrontRight.setPower(0);
            }
        } */

    /**
     * @param deltaAngle must be between 0 and 359.9
     */
    protected void turnRight(float deltaAngle) {
        assert (deltaAngle > 0.0);
        assert (deltaAngle <= 360.0);

        float currentAngle = getGyroscopeAngle();
        float destinationAngle = currentAngle + deltaAngle;
        turnRightAbsolute(destinationAngle);
    }

    /**
     * @param deltaAngle
     */
    protected void turnLeft(float deltaAngle) {
        assert (deltaAngle > 0.0);
        assert (deltaAngle <= 360.0);

        float currentAngle = getGyroscopeAngle();
        float destinationAngle = currentAngle - deltaAngle;
        turnLeftAbsolute(destinationAngle);
    }

    private float calculateRightDiff(float measuredDiff) {
        float ret = measuredDiff;
        while (ret < 0) {
            ret += 360.0;
        }
        return ret;
    }

    private float calculateLeftDiff(float measuredDiff) {
        float ret = measuredDiff;
        while (ret > 0) {
            ret -= 360.0;
        }
        return -ret;
    }

    protected void  turnRightAbsolutePid(float destinationAngle) {

        destinationAngle = CrazyAngle.normalizeAngle(destinationAngle); // between 0.0-359.999
        float currentAngle = getGyroscopeAngle(); // between 0.0-359.999
        double baseSpeed = config.getTurnSpeed();

        // destinationDiffAngle is going to tbe the number of degrees we still need to turn. Note tht if we have to transition over
        // the 360->0 boundary, then this number will be negative.
        float destinationDiffAngle = (destinationAngle - currentAngle);
        float diffRight = calculateRightDiff(destinationDiffAngle);
        float diffLeft = calculateLeftDiff(destinationDiffAngle);

        // Init debug info for printStatus().
        motorTurnType = "right";
        motorTurnDestination = destinationAngle;
        motorTurnAngleToGo = destinationDiffAngle;
        motorTurnAngleAdjustedToGo = diffRight;

        PIDController pidRotate =  new PIDController(.003, .00003, 0);
        pidRotate.reset();
        pidRotate.setSetpoint(0);
        pidRotate.setInputRange(0, diffRight);
        pidRotate.setOutputRange(0.4, baseSpeed);
        pidRotate.setTolerance(1);
        pidRotate.enable();

        // we continue in this loop as long as we still need to transition over the 360->0 boundary, or until we are within NUDGE_ANGLE degrees of the target.
        //while ((diffLeft > NUDGE_ANGLE) && (diffRight > NUDGE_ANGLE)) {
        while (!pidRotate.onTarget()) {
            double oldAngle = currentAngle;

            double currentPower = pidRotate.performPID(diffRight);
            nudgeRight(currentPower);

            currentAngle = getGyroscopeAngle();
            destinationDiffAngle = (destinationAngle - currentAngle);
            diffRight = calculateRightDiff(destinationDiffAngle);
            diffLeft = calculateLeftDiff(destinationDiffAngle);
           // motorTurnAngleToGo = destinationDiffAngle;
           // motorTurnAngleAdjustedToGo = diffRight;
            motorTurnAngleAdjustedToGo = (float) currentPower;
        }

        // Turn off the motor.
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setPower(0);
            motorFrontRight.setPower(0);
        }

        // Turn off debug info.
        motorTurnType = "right";
        motorTurnDestination = destinationAngle;
        motorTurnAngleToGo = destinationDiffAngle;
        motorTurnAngleAdjustedToGo = diffRight;
    }

    protected void  turnRightAbsolute(float destinationAngle) {
        destinationAngle = CrazyAngle.normalizeAngle(destinationAngle); // between 0.0-359.999
        float currentAngle = getGyroscopeAngle(); // between 0.0-359.999

        // destinationDiffAngle is going to tbe the number of degrees we still need to turn. Note tht if we have to transition over
        // the 360->0 boundary, then this number will be negative.
        float destinationDiffAngle = (destinationAngle - currentAngle);
        float diffRight = calculateRightDiff(destinationDiffAngle);
        float diffLeft = calculateLeftDiff(destinationDiffAngle);

        // Init debug info for printStatus().
        motorTurnType = "right";
        motorTurnDestination = destinationAngle;
        motorTurnAngleToGo = destinationDiffAngle;
        motorTurnAngleAdjustedToGo = diffRight;

        // we continue in this loop as long as we still need to transition over the 360->0 boundary, or until we are within NUDGE_ANGLE degrees of the target.
        while ((diffLeft > NUDGE_ANGLE) && (diffRight > NUDGE_ANGLE)) {
            float oldAngle = currentAngle;

            double power = config.getTurnSpeed();
            if (diffRight < (NUDGE_ANGLE * 5))
                power *= 0.5;
            else if (diffRight < (NUDGE_ANGLE * 4))
                power *= 0.25;
            nudgeRight(power);

            currentAngle = getGyroscopeAngle();
            destinationDiffAngle = (destinationAngle - currentAngle);
            diffRight = calculateRightDiff(destinationDiffAngle);
            diffLeft = calculateLeftDiff(destinationDiffAngle);
            motorTurnAngleToGo = destinationDiffAngle;
            motorTurnAngleAdjustedToGo = diffRight;
        }

        // Turn off the motor.
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setPower(0);
            motorFrontRight.setPower(0);
        }

        // Turn off debug info.
        motorTurnType = "none";
        motorTurnDestination = 0.0f;
        motorTurnAngleToGo = 0.0f;
        motorTurnAngleAdjustedToGo = 0.0f;
    }

    protected void  turnLeftAbsolute(float destinationAngle) {
        destinationAngle = CrazyAngle.normalizeAngle(destinationAngle); // between 0.0-359.999
        float currentAngle = getGyroscopeAngle(); // between 0.0-359.999

        // destinationDiffAngle is going to tbe the number of degrees we still need to turn. Note tht if we have to transition over
        // the 360->0 boundary, then this number will be negative.
        float destinationDiffAngle = (destinationAngle - currentAngle);
        float diffRight = calculateRightDiff(destinationDiffAngle);
        float diffLeft = calculateLeftDiff(destinationDiffAngle);

        // Init debug info for printStatus().
        motorTurnType = "left";
        motorTurnDestination = destinationAngle;
        motorTurnAngleToGo = destinationDiffAngle;
        motorTurnAngleAdjustedToGo = diffLeft;

        // we continue in this loop as long as we still need to transition over the 360->0 boundary, or until we are within NUDGE_ANGLE degrees of the target.
        while ((diffLeft > NUDGE_ANGLE) && (diffRight > NUDGE_ANGLE)) {
            float oldAngle = currentAngle;

            double power = config.getTurnSpeed();
            if (diffLeft < (NUDGE_ANGLE * 5))
                power *= 0.5;
            else if (diffLeft < (NUDGE_ANGLE * 4))
                power *= 0.25;
            nudgeLeft(power);

            currentAngle = getGyroscopeAngle();
            destinationDiffAngle = (destinationAngle - currentAngle);
            diffRight = calculateRightDiff(destinationDiffAngle);
            diffLeft = calculateLeftDiff(destinationDiffAngle);
            motorTurnAngleToGo = destinationDiffAngle;
            motorTurnAngleAdjustedToGo = diffLeft;
        }

        // Turn off the motor.
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setPower(0);
            motorFrontRight.setPower(0);
        }

        // Turn off debug info.
        motorTurnType = "none";
        motorTurnDestination = 0.0f;
        motorTurnAngleToGo = 0.0f;
        motorTurnAngleAdjustedToGo = 0.0f;
    }

    protected void turnToAngle(float destinationAngle) {
        float currentAngle;
        float angleDiff;
        currentAngle = getGyroscopeAngle();
        angleDiff = getAngleDifference(currentAngle, destinationAngle);
        if(angleDiff < 0) {
            turnLeftAbsolute(destinationAngle);
        } else {
            turnRightAbsolute(destinationAngle);
        }

    }

    // This nudges over about 2 degrees.
    protected void nudgeRight() {
        nudgeRight(config.getTurnSpeed());
        sleep(NUDGE_TIME);

        // Turn off the motor
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setPower(0);
            motorFrontRight.setPower(0);
        }
    }

    protected void nudgeRight(double power) {
        motorBackLeft.setPower(power);
        motorBackRight.setPower(-power);
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setPower(power);
            motorFrontRight.setPower(-power);
        }
    }

    // This nudges over about 2 degrees.
    protected void nudgeLeft() {
        nudgeLeft(config.getTurnSpeed());
        sleep(NUDGE_TIME);

        // Turn off the motor
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setPower(0);
            motorFrontRight.setPower(0);
        }
    }

    protected void nudgeLeft(double power) {
        motorBackLeft.setPower(-power);
        motorBackRight.setPower(power);
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setPower(-power);
            motorFrontRight.setPower(power);
        }
    }


    protected void nudgeBack() {
        double power = config.getTurnSpeed();

        motorBackLeft.setPower(-power);
        motorBackRight.setPower(-power);
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setPower(-power);
            motorFrontRight.setPower(-power);
        }
        sleep(500);

        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setPower(0);
            motorFrontRight.setPower(0);
        }
    }


    protected void pointToZero() {

        float currentAngle = getGyroscopeAngle();
        float destinationAngle = 0;
        boolean keepGoing = true;
        while (keepGoing) {
            float oldAngle = currentAngle;
            nudgeRight();
            currentAngle = getGyroscopeAngle();

            float justMoved = currentAngle - oldAngle;
            float stillNeed = 360.0f - currentAngle;
            telemetry.addData("turRight1", "current=%.0f, old=%.0f, dst=%.0f, moved=%.0f, need=%.0f", currentAngle, oldAngle, destinationAngle, justMoved, stillNeed);
            telemetry.update();

            keepGoing = (justMoved > -50.0);
        }
    }

    protected void strafeLeft(int numberOfMillis) {
        double power = config.getTurnSpeed();

        motorBackLeft.setPower(power);
        motorBackRight.setPower(-power);
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setPower(-power);
            motorFrontRight.setPower(power);
        }
        sleep(numberOfMillis);

        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setPower(0);
            motorFrontRight.setPower(0);
        }
    }


    protected void strafeRight(int numberOfMillis) {
        double power = config.getTurnSpeed();

        motorBackLeft.setPower(-power);
        motorBackRight.setPower(power);
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setPower(power);
            motorFrontRight.setPower(-power);
        }
        sleep(numberOfMillis);

        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
        if (config.getUseFourWheelDrive()) {
            motorFrontLeft.setPower(0);
            motorFrontRight.setPower(0);
        }
    }


    /*protected void lyftDownWalle(int howManySpins) {
        double speed = 0.5f;

        // Get the current position.
        int lyftBegin = wasteAllocationLoadLifterEarth.getCurrentPosition();
        telemetry.addData("lyftDownWalle", "Starting %7d", lyftBegin);

        // Determine new target position, and pass to motor controller
        int lyftTarget = lyftBegin + howManySpins;
        wasteAllocationLoadLifterEarth.setTargetPosition(lyftTarget);
        telemetry.addData("lyftDownWalle", "Target %7d", lyftTarget);

        // Turn On RUN_TO_POSITION
        wasteAllocationLoadLifterEarth.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        wasteAllocationLoadLifterEarth.setPower(speed);

        ElapsedTime motorOnTime = new ElapsedTime();
        while ((motorOnTime.seconds() < 30) && wasteAllocationLoadLifterEarth.isBusy()) {
            telemetry.addData("lyftDownWalle", "Running at %7d to %7d", wasteAllocationLoadLifterEarth.getCurrentPosition(), lyftTarget);
            telemetry.update();
            sleep(10);
        }

        // Turn off RUN_TO_POSITION
        wasteAllocationLoadLifterEarth.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        wasteAllocationLoadLifterEarth.setPower(0);

        //sleep(5000);
    } */
}
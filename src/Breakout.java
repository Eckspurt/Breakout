import com.almasb.ents.Entity;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.entity.Entities;
import com.almasb.fxgl.entity.GameEntity;
import com.almasb.fxgl.entity.component.CollidableComponent;
import com.almasb.fxgl.entity.component.TypeComponent;
import com.almasb.fxgl.gameplay.Level;
import com.almasb.fxgl.input.*;
import com.almasb.fxgl.parser.TextLevelParser;
import com.almasb.fxgl.physics.*;
import com.almasb.fxgl.settings.GameSettings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;

public class Breakout extends GameApplication
{
    private GameEntity paddle, ball;
    private PaddleControl paddleControl;
    private IntegerProperty lvl, balls, bricks, score;
    private TextLevelParser parser;
    private Level level;

    @Override
    protected void initSettings(GameSettings gameSettings)
    {
        gameSettings.setTitle("Breakout");
        gameSettings.setVersion("0.1");
        gameSettings.setWidth(640);
        gameSettings.setHeight(700);
        gameSettings.setIntroEnabled(false);
        gameSettings.setMenuEnabled(false);
        gameSettings.setShowFPS(false);
    }

    @Override
    protected void initInput()
    {
        Input input = getInput();

        input.addAction(new UserAction("Left")
        {
            @Override
            protected void onAction()
            {
                paddleControl.left();
            }

            @Override
            protected void onActionEnd()
            {
                paddleControl.stop();
            }
        }, KeyCode.A);

        input.addAction(new UserAction("Right")
        {
            @Override
            protected void onAction()
            {
                paddleControl.right();
            }

            @Override
            protected void onActionEnd()
            {
                paddleControl.stop();
            }
        }, KeyCode.D);


        input.addAction(new UserAction("Clear")
        {
            @Override
            protected void onActionEnd()
            {
                bricks.set(0);
                for(Entity brick : getGameWorld().getEntitiesByType(Type.BRICK))
                    brick.removeFromWorld();
            }
        }, KeyCode.C);
    }


    @Override
    protected void initAssets()
    {
        getAssetLoader().cache();
    }

    @Override
    protected void initGame()
    {
        parser = new TextLevelParser();
        parser.addEntityProducer('B', EntityFactory::newBrick);

        lvl = new SimpleIntegerProperty();
        level = parser.parse("breakout" + lvl.get() + ".txt");

        balls = new SimpleIntegerProperty(3);

        score = new SimpleIntegerProperty();    // starts at 0

        initScreenBounds();
        initPaddle();
        initBall();
        initBricks();
    }

    private void initScreenBounds()
    {
        Entity walls = Entities.makeScreenBounds(120);
        walls.addComponent(new TypeComponent(Type.SCREEN));
        walls.addComponent(new CollidableComponent(true));

        GameEntity bottom = Entities.builder().type(Type.BOTTOM).build();
        bottom.setY(getHeight());
        bottom.getBoundingBoxComponent().addHitBox(new HitBox("BODY", BoundingShape.box(getWidth(), 120)));

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);
        bottom.addComponent(physics);
        bottom.addComponent(new CollidableComponent(true));

        getGameWorld().addEntities(walls, bottom);
    }

    private void initPaddle()
    {
        paddle = Entities.builder().type(Type.PADDLE).build();
        paddle.setViewFromTextureWithBBox("paddle.png");
        paddle.setX(getWidth() / 2 - 128 / 2);
        paddle.setY(getHeight() -  24);

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.KINEMATIC);
        paddle.addComponent(physics);
        paddle.addComponent(new CollidableComponent(true));

        paddleControl = new PaddleControl();
        paddle.addControl(paddleControl);

        getGameWorld().addEntity(paddle);
    }

    private void initBall()
    {
        PhysicsComponent physics = new PhysicsComponent();

        physics.setBodyType(BodyType.DYNAMIC);

        FixtureDef def = new FixtureDef();
        def.setDensity(0.3f);
        def.setRestitution(1.0f);

        physics.setFixtureDef(def);
        physics.setOnPhysicsInitialized(() -> physics.setLinearVelocity(5, -5));

        ball = Entities.builder().type(Type.BALL).build();
        ball.getBoundingBoxComponent().addHitBox(new HitBox("BODY", BoundingShape.circle(12)));
        ball.setX(getWidth() / 2 - 24 / 2);
        ball.setY(getHeight() / 2 - 24 / 2);
        ball.setViewFromTexture("ball.png");

        ball.addComponent(physics);
        ball.addComponent(new CollidableComponent(true));
        ball.addControl(new BallControl());

        getGameWorld().addEntity(ball);
    }

    private void initBricks()
    {
        long numBricks = level.getEntities()
                .stream()
                .filter(e -> Entities.getType(e).isType(Type.BRICK))
                .count();

        bricks = new SimpleIntegerProperty();
        bricks.setValue(numBricks);

        level.getEntities()
                .stream()
                .filter(e -> Entities.getType(e).isType(Type.BRICK))
                .map(e -> Entities.getPosition(e).getValue())
                .forEach(point -> {
                    double x = point.getX() * 64;
                    double y = 30 + point.getY() * 32;

                    getGameWorld().addEntity(EntityFactory.newBrick(x, y));
                });
    }

    @Override
    protected void initPhysics()
    {
        getPhysicsWorld().setGravity(0, 0);

        getPhysicsWorld().addCollisionHandler(new CollisionHandler(Type.BALL, Type.BRICK)
        {
            @Override
            protected void onCollisionBegin(Entity a, Entity b)
            {
                b.removeFromWorld();
                bricks.set(bricks.get() - 1);
                score.setValue(score.get() + 100);

                if(bricks.get() <= 0)
                {
                    lvl.set(lvl.get() + 1);
                    level = parser.parse("breakout" + lvl.get() + ".txt");
                    initBricks();
                }
            }
        });

        getPhysicsWorld().addCollisionHandler(new CollisionHandler(Type.BALL, Type.PADDLE)
        {
            @Override
            protected void onCollisionBegin(Entity a, Entity b)
            {
                PhysicsComponent physics =
                        ball.getComponentUnsafe(PhysicsComponent.class);

                double x;

                if(ball.getX() + 24 / 2 > paddle.getX() + 128 / 2)
                    x = Math.abs(physics.getLinearVelocity().getX());
                else
                    x = -Math.abs(physics.getLinearVelocity().getX());

                physics.setLinearVelocity(x, physics.getLinearVelocity().getY());
            }
        });

        getPhysicsWorld().addCollisionHandler(new CollisionHandler(Type.BALL, Type.BOTTOM)
        {
            @Override
            protected void onCollisionBegin(Entity a, Entity b)
            {
                a.removeFromWorld();
                balls.set(balls.get() - 1);
                if(balls.get() > 0)
                    initBall();
                else
                    gameOver();
            }
        });
    }

    @Override
    protected void initUI()
    {
        Text scoreText = new Text();
        scoreText.setTranslateX(5);
        scoreText.setTranslateY(20);
        scoreText.setFont(Font.font(18));
        scoreText.textProperty().bind(score.asString("Score: %d"));

        Text ballsText = new Text();
        ballsText.setTranslateX(getWidth() - 70);
        ballsText.setTranslateY(20);
        ballsText.setFont(Font.font(18));
        ballsText.textProperty().bind(balls.asString("Balls: %d"));

        Text bricksText = new Text();
        bricksText.setTranslateX(getWidth() / 2);
        bricksText.setTranslateY(20);
        bricksText.setFont(Font.font(18));
        bricksText.textProperty().bind(bricks.asString("Bricks: %d"));

        getGameScene().addUINodes(scoreText, ballsText, bricksText);
    }

    @Override
    protected void onUpdate(double v)
    {
        if(balls.get() <= 0)
            gameOver();
        else if(getGameWorld().getEntitiesByType(Type.BALL).isEmpty())
            initBall();
    }

    private void gameOver()
    {
        int fontSize = 36;

        Text gameOver = new Text("Game Over!");
        int xOffset = gameOver.getText().length() / 2 * fontSize / 2;

        gameOver.setX(getWidth() / 2 - xOffset);
        gameOver.setY(getHeight() / 2);
        gameOver.setFont(Font.font(fontSize));

        getGameScene().addUINodes(gameOver);
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
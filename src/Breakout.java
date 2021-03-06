import com.almasb.ents.Entity;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.entity.Entities;
import com.almasb.fxgl.entity.GameEntity;
import com.almasb.fxgl.entity.component.CollidableComponent;
import com.almasb.fxgl.entity.component.PositionComponent;
import com.almasb.fxgl.entity.component.TypeComponent;
import com.almasb.fxgl.input.*;
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
    private enum Type
    {
        PADDLE, BALL, BRICK, SCREEN, BOTTOM
    }

    private GameEntity paddle, ball;
    private PaddleControl paddleControl;
    private IntegerProperty bricks, score;

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
    }


    @Override
    protected void initAssets()
    {
        getAssetLoader().cache();
    }

    @Override
    protected void initGame()
    {
        score = new SimpleIntegerProperty();    // starts at 0

        bricks = new SimpleIntegerProperty();
        bricks.set(30);

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
        for(int i = 0; i < bricks.get(); i++)
        {
            GameEntity brick = Entities.builder().type(Type.BRICK).build();
            brick.setX(i % 10 * 64);
            brick.setY(30 + i / 10 * 32);
            brick.setViewFromTextureWithBBox("brick.png");
            brick.addComponent(new CollidableComponent(true));

            PhysicsComponent physics = new PhysicsComponent();
            physics.setBodyType(BodyType.STATIC);
            brick.addComponent(physics);

            getGameWorld().addEntity(brick);
        }
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
                score.setValue(score.get() + 100);
            }
        });

        getPhysicsWorld().addCollisionHandler(new CollisionHandler(Type.BALL, Type.PADDLE)
        {
            @Override
            protected void onCollisionBegin(Entity a, Entity b)
            {
                PositionComponent aPosition = a.getComponentUnsafe(PositionComponent.class);
                PositionComponent bPosition = b.getComponentUnsafe(PositionComponent.class);

                PhysicsComponent physics = a.getComponentUnsafe(PhysicsComponent.class);

                if(aPosition.getX() + 24 / 2 > bPosition.getX() + 128 / 2)
                    physics.setLinearVelocity(5, physics.getLinearVelocity().getY());
                else
                    physics.setLinearVelocity(-5, physics.getLinearVelocity().getY());
            }
        });

        getPhysicsWorld().addCollisionHandler(new CollisionHandler(Type.BALL, Type.BOTTOM)
        {
            @Override
            protected void onCollisionBegin(Entity a, Entity b)
            {
                a.removeFromWorld();
                initBall();
            }
        });
    }

    @Override
    protected void initUI()
    {
        Text scoreText = new Text();
        scoreText.setTranslateY(20);
        scoreText.setFont(Font.font(18));
        scoreText.textProperty().bind(score.asString("Score: %d"));

        getGameScene().addUINode(scoreText);
    }

    @Override
    protected void onUpdate(double v)
    {

    }

    public static void main(String[] args)
    {
        launch(args);
    }
}

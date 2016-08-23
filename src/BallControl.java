import com.almasb.ents.AbstractControl;
import com.almasb.ents.Entity;
import com.almasb.fxgl.physics.PhysicsComponent;

public class BallControl extends AbstractControl
{
    private PhysicsComponent physics;

    @Override
    public void onAdded(Entity entity)
    {
        physics = entity.getComponentUnsafe(PhysicsComponent.class);
    }

    @Override
    public void onUpdate(Entity entity, double v)
    {
        if(Math.abs(physics.getLinearVelocity().getX()) < 5)
            physics.setLinearVelocity(Math.signum(physics.getLinearVelocity().getX()) * 5,
                    physics.getLinearVelocity().getY());

        if(Math.abs(physics.getLinearVelocity().getY()) < 5)
            physics.setLinearVelocity(physics.getLinearVelocity().getX(),
                    Math.signum(physics.getLinearVelocity().getY()) * 5);
    }
}
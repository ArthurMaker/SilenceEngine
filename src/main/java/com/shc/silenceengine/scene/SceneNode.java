package com.shc.silenceengine.scene;

import com.shc.silenceengine.core.SilenceException;
import com.shc.silenceengine.entity.Entity2D;
import com.shc.silenceengine.graphics.Batcher;
import com.shc.silenceengine.graphics.opengl.GL3Context;
import com.shc.silenceengine.math.Transform;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Sri Harsha Chilakapati
 */
public class SceneNode
{
    private List<SceneNode> children;
    private List<SceneComponent> components;
    private SceneNode       parent;
    private Transform       transform;
    private boolean         destroyed;

    private static int idGenerator = 0;
    private int id;

    public SceneNode()
    {
        children = new ArrayList<>();
        components = new ArrayList<>();
        transform = new Transform();
        parent = null;

        id = ++idGenerator;
    }

    public void preInit()
    {
        init();
        initChildren();
    }

    public void init()
    {
    }

    private void initChildren()
    {
        for (int i = 0; i < children.size(); i++)
            children.get(i).preInit();
    }

    public void addChild(SceneNode child)
    {
        if (child.getParent() != null)
            throw new SilenceException("A WorldComponent can be a child of a single parent!");

        children.add(child);
        child.setParent(this);
        child.init();

        if (child instanceof Entity2D)
        {
            // Sort the Entity2D's in children based on their depth
            Collections.sort(children, (SceneNode c1, SceneNode c2) ->
            {
                if (c1 instanceof Entity2D && c2 instanceof Entity2D)
                    return ((Integer) ((Entity2D) c2).getDepth()).compareTo(((Entity2D) c1).getDepth());

                return 0;
            });
        }
    }

    public void addComponent(SceneComponent component)
    {
        components.add(component);
    }

    public void removeComponent(SceneComponent component)
    {
        if (components.contains(component))
        {
            components.remove(component);
            component.dispose();
        }
    }

    public void preUpdate(float delta)
    {
        update(delta);
        updateComponents(delta);
        updateChildren(delta);
    }

    public void update(float delta)
    {
    }

    protected void updateComponents(float delta)
    {
        for (SceneComponent component : components)
            component.update(delta);
    }

    protected void updateChildren(float delta)
    {
        for (int i = 0; i < children.size(); i++)
        {
            SceneNode child = children.get(i);
            child.preUpdate(delta);

            if (child.isDestroyed())
            {
                removeChild(child);
                i--;
            }
        }
    }

    public void preRender(float delta, Batcher batcher)
    {
        render(delta, batcher);
        renderChildren(delta, batcher);
    }

    protected void renderChildren(float delta, Batcher batcher)
    {
        if (components.size() == 0)
            doRenderChildren(delta, batcher);
        else
            renderChildrenWithComponents(delta, batcher);
    }

    public void render(float delta, Batcher batcher)
    {
    }

    private void doRenderChildren(float delta, Batcher batcher)
    {
        for (int i = 0; i < children.size(); i++)
        {
            SceneNode child = children.get(i);
            child.preRender(delta, batcher);

            if (child.isDestroyed())
            {
                removeChild(child);
                i--;
            }
        }
    }

    private void renderChildrenWithComponents(float delta, Batcher batcher)
    {
        // Enable forward rendering
        GL3Context.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL3Context.depthMask(false);
        GL3Context.depthFunc(GL11.GL_EQUAL);

        for (SceneComponent component : components)
        {
            component.use();
            doRenderChildren(delta, batcher);
            component.release();
        }

        // Disable forward rendering
        GL3Context.depthFunc(GL11.GL_LESS);
        GL3Context.depthMask(true);
        GL3Context.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void removeChild(SceneNode child)
    {
        if (child.getParent() != this)
            throw new SilenceException("Cannot remove non-existing Child!");

        child.destroy();
        children.remove(child);
        child.setParent(null);
    }

    public void removeChildren()
    {
        for (int i = 0; i < children.size(); i++)
        {
            removeChild(children.get(i));
            i--;
        }
    }

    public void destroy()
    {
        destroyed = true;
        destroyChildren();
        destroyComponents();
    }

    public void destroyChildren()
    {
        children.forEach(SceneNode::destroy);
        children.clear();
    }

    public void destroyComponents()
    {
        components.forEach(SceneComponent::dispose);
        components.clear();
    }

    public List<SceneNode> getChildren()
    {
        return children;
    }

    public List<SceneComponent> getComponents()
    {
        return components;
    }

    public SceneNode getParent()
    {
        return parent;
    }

    private void setParent(SceneNode parent)
    {
        this.parent = parent;
    }

    public Transform getTransform()
    {
        if (getParent() != null)
            return transform.copy().apply(parent.getTransform());
        else
            return transform;
    }

    public Transform getLocalTransform()
    {
        return transform;
    }

    public boolean isDestroyed()
    {
        return destroyed;
    }

    public int getID()
    {
        return id;
    }
}

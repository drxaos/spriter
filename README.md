# Spriter
A simple 2d sprites engine

## Example
```java
public class Simple {
    public static void main(String[] args) throws Exception {
        Spriter spriter = new Spriter("Simple");

        Spriter.Sprite sprite = spriter.createSprite(
                ImageIO.read(Animation.class.getResource("/point.png")), // load image
                256 / 2, 256 / 2,   // sprite center
                0.2                 // object size
        );

        Spriter.Control control = spriter.getControl();

        while (true) {
            sprite.setPos(control.getMousePos());
            Thread.sleep(30);
        }
    }
}
```
![CustomUI](/spriter-simple.png)

## Screenshots

![CustomUI](/spriter-composition.png)

![CustomUI](/spriter-customui.png)

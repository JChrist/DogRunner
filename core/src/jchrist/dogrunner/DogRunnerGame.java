package jchrist.dogrunner;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import jchrist.dogrunner.utils.GifDecoder;

public class DogRunnerGame extends ApplicationAdapter {
	public static final float DOG_JUMP_IMPULSE = 200;
	public static final float GRAVITY = -10;
	public static final float DOG_VELOCITY_X = 200;
	public static final float DOG_START_Y = 240;
	public static final float DOG_START_X = 50;

	private ShapeRenderer shapeRenderer;
	private SpriteBatch batch;
	private OrthographicCamera camera;
	private OrthographicCamera uiCamera;
	private Texture background;
	private TextureRegion ground;
	private float groundOffsetX = 0;
	private TextureRegion rock;
	private TextureRegion rockDown;
	private Animation dog;
	private TextureRegion ready;
	private TextureRegion gameOver;
	private BitmapFont font;

	private Vector2 dogPosition = new Vector2();
	private Vector2 dogVelocity = new Vector2();
	private float dogStateTime = 0f;

	private Vector2 gravity = new Vector2();
	private Array<Rock> rocks = new Array<>();

	private GameState gameState = GameState.Start;
	private int score = 0;
	private Rectangle rect1 = new Rectangle();
	private Rectangle rect2 = new Rectangle();
	private Music music;
	private Sound explode;

	@Override
	public void create () {
		shapeRenderer = new ShapeRenderer();
		batch = new SpriteBatch();
		camera = new OrthographicCamera();
		camera.setToOrtho(false, 800, 480);
		uiCamera = new OrthographicCamera();
		uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		uiCamera.update();

		font = new BitmapFont(Gdx.files.internal("arial.fnt"));

		background = new Texture("background.png");
		ground = new TextureRegion(new Texture("ground.png"));

		rock = new TextureRegion(new Texture("rock.png"));
		rockDown = new TextureRegion(rock);
		rockDown.flip(false, true);

		ready = new TextureRegion(new Texture("ready.png"));
		gameOver = new TextureRegion(new Texture("gameover.png"));

		dog = GifDecoder.loadGIFAnimation(PlayMode.LOOP, 0.05f, Gdx.files.internal("dog.gif").read());

		music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));
		music.setLooping(true);
		music.play();

		explode = Gdx.audio.newSound(Gdx.files.internal("explode.wav"));

		resetWorld();
	}

	private void resetWorld() {
		score = 0;
		groundOffsetX = 0;
		dogPosition.set(DOG_START_X, DOG_START_Y);
		dogVelocity.set(0, 0);
		gravity.set(0, GRAVITY);
		camera.position.x = 400;

		rocks.clear();
		for(int i = 0; i < 5; i++) {
			boolean isDown = MathUtils.randomBoolean();
			rocks.add(new Rock(700 + i * 200, isDown?500-rock.getRegionHeight(): 0, isDown? rockDown: rock));
		}
	}

	private void updateWorld() {
		float deltaTime = Gdx.graphics.getDeltaTime();
		dogStateTime += deltaTime;

		if(Gdx.input.justTouched()) {
			if(gameState == GameState.Start) {
				gameState = GameState.Running;
			}
			if(gameState == GameState.Running) {
				dogVelocity.set(DOG_VELOCITY_X, DOG_JUMP_IMPULSE);
			}
			if(gameState == GameState.GameOver) {
				gameState = GameState.Start;
				resetWorld();
			}
		}

		if(gameState != GameState.Start) dogVelocity.add(gravity);

		dogPosition.mulAdd(dogVelocity, deltaTime);

		camera.position.x = dogPosition.x + 350;
		if(camera.position.x - groundOffsetX > ground.getRegionWidth() + 400) {
			groundOffsetX += ground.getRegionWidth();
		}

		rect1.set(dogPosition.x + 20, dogPosition.y, dog.getKeyFrames()[0].getRegionWidth() - 20, dog.getKeyFrames()[0].getRegionHeight());
		for(Rock r: rocks) {
			if(camera.position.x - r.position.x > 400 + r.image.getRegionWidth()) {
				boolean isDown = MathUtils.randomBoolean();
				r.position.x += 5 * 200;
				r.position.y = isDown?500-rock.getRegionHeight(): 0;
				r.image = isDown? rockDown: rock;
				r.counted = false;
			}
			rect2.set(r.position.x + (r.image.getRegionWidth() - 30) / 2 + 20, r.position.y, 20, r.image.getRegionHeight() - 10);
			if(rect1.overlaps(rect2)) {
				if(gameState != GameState.GameOver) explode.play();
				gameState = GameState.GameOver;
				dogVelocity.x = 0;
			}
			if(r.position.x < dogPosition.x && !r.counted) {
				score++;
				r.counted = true;
			}
		}

		if(dogPosition.y < ground.getRegionHeight() - 40 ||
				dogPosition.y + dog.getKeyFrames()[0].getRegionHeight() > 500 - ground.getRegionHeight() + 20) {
			if(gameState != GameState.GameOver) explode.play();
			gameState = GameState.GameOver;
			dogVelocity.x = 0;
		}
	}

	private void drawWorld() {
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		batch.draw(background, camera.position.x - background.getWidth() / 2, 0);
		for(Rock rock: rocks) {
			batch.draw(rock.image, rock.position.x, rock.position.y);
		}
		batch.draw(ground, groundOffsetX, 0);
		batch.draw(ground, groundOffsetX + ground.getRegionWidth(), 0);
		batch.draw(dog.getKeyFrame(dogStateTime), dogPosition.x, dogPosition.y);
		batch.end();

		batch.setProjectionMatrix(uiCamera.combined);
		batch.begin();
		if(gameState == GameState.Start) {
			batch.draw(ready, Gdx.graphics.getWidth() / 2 - ready.getRegionWidth() / 2, Gdx.graphics.getHeight() / 2 - ready.getRegionHeight() / 2);
		}
		if(gameState == GameState.GameOver) {
			batch.draw(gameOver, Gdx.graphics.getWidth() / 2 - gameOver.getRegionWidth() / 2, Gdx.graphics.getHeight() / 2 - gameOver.getRegionHeight() / 2);
		}
		if(gameState == GameState.GameOver || gameState == GameState.Running) {
			font.draw(batch, "" + score, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() - 60);
		}
		batch.end();
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(1, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		updateWorld();
		drawWorld();
	}

	static class Rock {
		Vector2 position = new Vector2();
		TextureRegion image;
		boolean counted;

		public Rock(float x, float y, TextureRegion image) {
			this.position.x = x;
			this.position.y = y;
			this.image = image;
		}
	}

	static enum GameState {
		Start, Running, GameOver
	}
}

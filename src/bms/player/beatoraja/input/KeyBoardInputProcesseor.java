package bms.player.beatoraja.input;

import java.util.Arrays;

import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.Resolution;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.Input.Keys;

/**
 * キーボード入力処理用クラス
 * 
 * @author exch
 */
public class KeyBoardInputProcesseor extends BMSPlayerInputDevice implements InputProcessor {

	private int[] keys = new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
			Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
			Keys.BACKSLASH, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT };
	private int[] control = new int[] { Keys.Q, Keys.W };

	private MouseScratchInput mouseScratchInput;
	/**
	 * 終了キー
	 */
	private final int exit = Keys.ESCAPE;
	/**
	 * ENTERキー
	 */
	private final int enter = Keys.ENTER;
	/**
	 * DELキー
	 */
	private final int delete = Keys.FORWARD_DEL;

	private final IntArray reserved;
	/**
	 * 最後に押されたキー
	 */
	private int lastPressedKey = -1;

	private boolean enable = true;

	/**
	 * 画面の解像度。マウスの入力イベント処理で使用
	 */
	private Resolution resolution;

	/**
	 * 各キーのon/off状態
	 */
	private final boolean[] keystate = new boolean[256];
	/**
	 * 各キーの状態変化時間
	 */
	private final long[] keytime = new long[256];
	/**
	 * キーの最少入力感覚
	 */
	private int duration;

	public KeyBoardInputProcesseor(BMSPlayerInputProcessor bmsPlayerInputProcessor, KeyboardConfig config, Resolution resolution) {
		super(bmsPlayerInputProcessor, Type.KEYBOARD);
		this.mouseScratchInput = new MouseScratchInput(bmsPlayerInputProcessor, this, config);
		this.setConfig(config);
		this.resolution = resolution;
		
		reserved = new IntArray();
		Arrays.stream(ControlKeys.values()).forEach(keys -> reserved.add(keys.keycode));
		reserved.addAll(exit);
		reserved.addAll(enter);
		
		Arrays.fill(keytime, Long.MIN_VALUE);
	}

	public void setConfig(KeyboardConfig config) {
		this.keys = config.getKeyAssign().clone();
		this.duration = config.getDuration();
		this.control = new int[] { config.getStart(), config.getSelect() };
		mouseScratchInput.setConfig(config);
	}

	public boolean keyDown(int keycode) {
		setLastPressedKey(keycode);
		return true;
	}

	public boolean keyTyped(char keycode) {
		return false;
	}

	public boolean keyUp(int keycode) {
		return true;
	}

	public void clear() {
		// Arrays.fill(keystate, false);
		Arrays.fill(keytime, Long.MIN_VALUE);
		lastPressedKey = -1;
		mouseScratchInput.clear();
	}

	public void poll(final long presstime) {
		if (enable) {
			for (int i = 0; i < keys.length; i++) {
				if(keys[i] < 0) {
					continue;
				}
				final boolean pressed = Gdx.input.isKeyPressed(keys[i]);
				if (pressed != keystate[keys[i]] && presstime >= keytime[keys[i]] + duration) {
					keystate[keys[i]] = pressed;
					keytime[keys[i]] = presstime;
					this.bmsPlayerInputProcessor.keyChanged(this, presstime, i, pressed);
					this.bmsPlayerInputProcessor.setAnalogState(i, false, 0);
				}
			}

			for (ControlKeys key : ControlKeys.values()) {
				final boolean pressed = Gdx.input.isKeyPressed(key.keycode);
				if (pressed != keystate[key.keycode]) {
					keystate[key.keycode] = pressed;
					keytime[key.keycode] = presstime;
				}
			}

			final boolean startpressed = Gdx.input.isKeyPressed(control[0]);
			if (startpressed != keystate[control[0]]) {
				keystate[control[0]] = startpressed;
				this.bmsPlayerInputProcessor.startChanged(startpressed);
			}
			final boolean selectpressed = Gdx.input.isKeyPressed(control[1]);
			if (selectpressed != keystate[control[1]]) {
				keystate[control[1]] = selectpressed;
				this.bmsPlayerInputProcessor.setSelectPressed(selectpressed);
			}
		}
		
		mouseScratchInput.poll(presstime);

		final boolean exitpressed = Gdx.input.isKeyPressed(exit);
		if (exitpressed != keystate[exit]) {
			keystate[exit] = exitpressed;
			this.bmsPlayerInputProcessor.setExitPressed(exitpressed);
		}
		final boolean enterpressed = Gdx.input.isKeyPressed(enter);
		if (enterpressed != keystate[enter]) {
			keystate[enter] = enterpressed;
			this.bmsPlayerInputProcessor.setEnterPressed(enterpressed);
		}
		final boolean deletepressed = Gdx.input.isKeyPressed(delete);
		if (deletepressed != keystate[delete]) {
			keystate[delete] = deletepressed;
			this.bmsPlayerInputProcessor.setDeletePressed(deletepressed);
		}
	}

	public boolean getKeyState(int keycode) {
		return keystate[keycode];
	}

	public boolean isKeyPressed(int keycode) {
		if(keystate[keycode] && keytime[keycode] != Long.MIN_VALUE) {
			keytime[keycode] = Long.MIN_VALUE;
			return true;
		}
		return false;
	}

	public boolean mouseMoved(int x, int y) {
		this.bmsPlayerInputProcessor.setMouseMoved(true);
		this.bmsPlayerInputProcessor.mousex = x * resolution.width / Gdx.graphics.getWidth();
		this.bmsPlayerInputProcessor.mousey = resolution.height - y * resolution.height / Gdx.graphics.getHeight();
		return false;
	}

	/**
	 * 旧InputProcessorのメソッド
	 * libGDX更新時に削除
	 */
	public boolean scrolled(int amount) {
		return scrolled(0, amount);
	}

	public boolean scrolled(float amountX, float amountY) {
		this.bmsPlayerInputProcessor.scrollX += amountX;
		this.bmsPlayerInputProcessor.scrollY += amountY;
		return false;
	}

	public boolean touchDown(int x, int y, int point, int button) {
		this.bmsPlayerInputProcessor.mousebutton = button;
		this.bmsPlayerInputProcessor.mousex = x * resolution.width / Gdx.graphics.getWidth();
		this.bmsPlayerInputProcessor.mousey = resolution.height - y * resolution.height
				/ Gdx.graphics.getHeight();
		this.bmsPlayerInputProcessor.mousepressed = true;
		return false;
	}

	public boolean touchDragged(int x, int y, int point) {
		this.bmsPlayerInputProcessor.mousex = x * resolution.width / Gdx.graphics.getWidth();
		this.bmsPlayerInputProcessor.mousey = resolution.height - y * resolution.height
				/ Gdx.graphics.getHeight();
		this.bmsPlayerInputProcessor.mousedragged = true;
		return false;
	}

	public boolean touchUp(int arg0, int arg1, int arg2, int arg3) {
		return false;
	}

	public int getLastPressedKey() {
		return lastPressedKey;
	}

	public void setLastPressedKey(int lastPressedKey) {
		this.lastPressedKey = lastPressedKey;
	}

	public MouseScratchInput getMouseScratchInput() {
		return mouseScratchInput;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}
	
	public boolean isReservedKey(int key) {
		return reserved.contains(key);
	}
	
	public enum ControlKeys {
		NUM0(0, Keys.NUM_0),
		NUM1(1, Keys.NUM_1),
		NUM2(2, Keys.NUM_2),
		NUM3(3, Keys.NUM_3),
		NUM4(4, Keys.NUM_4),
		NUM5(5, Keys.NUM_5),
		NUM6(6, Keys.NUM_6),
		NUM7(7, Keys.NUM_7),
		NUM8(8, Keys.NUM_8),
		NUM9(9, Keys.NUM_9),
		
		F1(10, Keys.F1),
		F2(11, Keys.F2),
		F3(12, Keys.F3),
		F4(13, Keys.F4),
		F5(14, Keys.F5),
		F6(15, Keys.F6),
		F7(16, Keys.F7),
		F8(17, Keys.F8),
		F9(18, Keys.F9),
		F10(19, Keys.F10),
		F11(20, Keys.F11),		
		F12(21, Keys.F12),
		
		UP(22, Keys.UP),
		DOWN(23, Keys.DOWN),
		LEFT(24, Keys.LEFT),
		RIGHT(25, Keys.RIGHT),
		
//		ENTER(26, Keys.ENTER),
//		DEL(27, Keys.FORWARD_DEL),
//		ESCAPE(28, Keys.ESCAPE),
		;
		
		public final int id;
		
		public final int keycode;
		
		private ControlKeys(int id, int keycode) {
			this.id = id;
			this.keycode = keycode;
		}
	}

}
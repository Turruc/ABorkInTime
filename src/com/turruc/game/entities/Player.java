package com.turruc.game.entities;

import java.awt.event.KeyEvent;

import com.turruc.engine.GameContainer;
import com.turruc.engine.Renderer;
import com.turruc.engine.audio.SoundClip;
import com.turruc.engine.gfx.ImageTile;
import com.turruc.game.GameManager;
import com.turruc.game.GameState;

public class Player extends GameObject {
	private ImageTile player = new ImageTile("/player.png", 32, 32);
	private ImageTile shield = new ImageTile("/shield.png", 32, 32);
	private int padding, paddingTop;

	private int direction = 0;
	private float anim = 0;
	private int tileX, tileY;
	private float offX, offY;

	private float normalSpeed = 200;
	private float slowSpeed = normalSpeed / slowMotion;
	private float speed = normalSpeed;

	private float fallDistance = 0;
	private float normalFallSpeed = 25;
	private float slowFallSpeed = normalFallSpeed / slowMotion;
	private float fallSpeed = normalFallSpeed;
	private float jump = -10; // must be negative
	private boolean ground = false;
	private boolean groundLast = false;

	private int maxHealth = 50;
	private int health = maxHealth;
	private float maxMana = 100;
	private float mana = 0;
	private float normalRechargeRate = 0;
	private float slowRechargeRate = normalRechargeRate / slowMotion;
	private float rechargeRate = normalRechargeRate;

	private int teleportRange = 200;
	private float teleportX = 0;
	private float teleportY = 0;

	private int teleportCost = 30;
	private double slowMotionCost = 0.5;
	private int slowMotionCooldown = 1; // in seconds

	private float ladderSpeed = (float) .5;

	private int normalAnimationSpeed = 20;
	private int slowAnimationSpeed = normalAnimationSpeed / slowMotion;
	private int animationSpeed = normalAnimationSpeed;

	private double lastTimeSlowUsed;

	private double lastTimeLavaDamage;
	private int lavaDamageCooldown = 1;
	private int lavaDamage = 200;

	private int spikeDamage = 200;

	private boolean slow = false;
	private boolean checkSlow = false;

	private int meleeDamage = 100;
	private int shootCost = 10;

	private boolean attacking = false;
	private float attackAnim = 4;

	private boolean shielded = false;
	private float shieldAnim = 4;

	private SoundClip ow;
	private SoundClip pew;
	private SoundClip woosh;
	private SoundClip boof;
	private SoundClip vshh;
	private SoundClip pop;

	public Player(int posX, int posY) {
		this.tag = EntityType.player;
		this.tileX = posX;
		this.tileY = posY;
		this.offX = 0;
		this.offY = 0;
		this.posX = posX * GameManager.TS;
		this.posY = posY * GameManager.TS;
		this.width = 32;
		this.height = 32;

		this.padding = 4;
		this.paddingTop = 0;

		if (ow == null) ow = new SoundClip("/audio/ow.wav");
		if (pew == null) pew = new SoundClip("/audio/pew.wav");
		if (woosh == null) woosh = new SoundClip("/audio/woosh.wav");
		if (boof == null) boof = new SoundClip("/audio/boof.wav");
		if (vshh == null) vshh = new SoundClip("/audio/vshh.wav");
		if (pop == null) pop = new SoundClip("/audio/pop.wav");
	}

	@Override
	public void update(GameContainer gc, float dt) {
		mana = 100;
		if (health == 0) {
			boof.play();
			shielded = false;
			gc.gameState = GameState.GAMEOVER;
			health = maxHealth;
			mana = 0;
			moveTo(3 * 32, 5 * 32);
		}

		// slow motion
		if (gc.getInput().isKey(KeyEvent.VK_CONTROL) && GameManager.gm.getPlayer().getMana() > GameManager.gm.getPlayer().getSlowMotionCost() && (System.nanoTime() / 1000000000.0) - lastTimeSlowUsed > slowMotionCooldown) {
			checkSlow = false;
			slow = true;
			mana -= slowMotionCost;
			rechargeRate = slowRechargeRate;
			speed = slowSpeed;
			fallSpeed = slowFallSpeed;
			animationSpeed = slowAnimationSpeed;
		} else {
			slow = false;
			rechargeRate = normalRechargeRate;
			speed = normalSpeed;
			fallSpeed = normalFallSpeed;
			animationSpeed = normalAnimationSpeed;
		}

		if (gc.getInput().isKeyUp(KeyEvent.VK_CONTROL) && !checkSlow) {
			lastTimeSlowUsed = System.nanoTime() / 1000000000.0;
			checkSlow = true;
		}

		// lava damage
		if (GameManager.gm.getCollisionNum((int) tileX, (int) tileY) == 3 && (System.nanoTime() / 1000000000.0) - lastTimeLavaDamage > lavaDamageCooldown) {
			lastTimeLavaDamage = System.nanoTime() / 1000000000.0;
			hit(lavaDamage);
		}
		// end lava damage

		// Lava Slow
		if (GameManager.gm.getCollisionNum((int) tileX, (int) tileY) == 3 && !slow) {
			speed = slowSpeed * 3;
			fallSpeed = slowFallSpeed * 3;
			animationSpeed = slowAnimationSpeed * 3;
		} else if (GameManager.gm.getCollisionNum((int) tileX, (int) tileY) == 3 && slow) {
			speed = (slowSpeed * 3) / slowMotion;
			fallSpeed = (slowFallSpeed * 3) / slowMotion;
			animationSpeed = (slowAnimationSpeed * 3) / slowMotion;
		} else if (!slow) {
			speed = normalSpeed;
			fallSpeed = normalFallSpeed;
			animationSpeed = normalAnimationSpeed;
		}
		// end lava slow

		// Spike Damage
		if (GameManager.gm.getCollisionNum((int) tileX, (int) tileY) == 6) {
			hit(spikeDamage);
		}
		// End of Spike Damage

		// Victory Door
		if (GameManager.gm.getCollisionNum((int) tileX, (int) tileY) == 7) {
			gc.gameState = GameState.VICTORY;
			health = maxHealth;
			mana = 0;
			moveTo(3 * 32, 5 * 32);
		}
		// End of Victory Door
		
		// resource balls
		if (GameManager.gm.getCollisionNum((int) tileX, (int) tileY) == -1 || GameManager.gm.getCollisionNum((int) tileX, (int) tileY) == -2) {

			for (int i = 0; i < GameManager.getObjects().size(); i++) {
				if (GameManager.getObjects().get(i).getTag().equals(EntityType.resourceBall)) {
					if (Math.abs(posX - GameManager.getObjects().get(i).getPosX()) <= 32 && Math.abs(posY - GameManager.getObjects().get(i).getPosY()) <= 32) {
						GameManager.getObjects().get(i).setDead(true);
						break;
						// i = GameManager.gm.getObjects().size();
					}
				}
			}

		}
		// end resource balls

		// health and mana check
		if (health > maxHealth) {
			health = maxHealth;
		} else if (health < 0) {
			health = 0;
		}

		mana += (GameManager.getGc().getDt() * rechargeRate); // mana regen
		if (mana > maxMana) {
			mana = maxMana;
		} else if (mana < 0) {
			mana = 0;
		}

		if (attacking) {
			attackAnim += dt * animationSpeed;
			if (attackAnim > 7) {
				attacking = false;
				attackAnim = 4;
			}
		}
		// end health and mana check

		// melee
		if (gc.getInput().isButtonDown(3) && !attacking) {
			attacking = true;
			vshh.play();
		}

		if (attacking) {
			if (direction == 0) {
				for (int i = 0; i < GameManager.getObjects().size(); i++) {
					if (GameManager.getObjects().get(i).getTag().equals(EntityType.turret) || GameManager.getObjects().get(i).getTag().equals(EntityType.meleeEnemy) || GameManager.getObjects().get(i).getTag().equals(EntityType.floatEnemy)) {
						if (checkContact(this.posX + 20, this.posY, GameManager.getObjects().get(i).getPosX(), GameManager.getObjects().get(i).getPosY())) {
							if (GameManager.getObjects().get(i).getTag().equals(EntityType.turret)) GameManager.getObjects().get(i).setDead(true); // kill
							// turret
							if (GameManager.getObjects().get(i).getTag().equals(EntityType.meleeEnemy)) ((MeleeEnemy) GameManager.getObjects().get(i)).hit(meleeDamage); // damage
							// meleeEnemy
							if (GameManager.getObjects().get(i).getTag().equals(EntityType.floatEnemy)) ((FloatEnemy) GameManager.getObjects().get(i)).hit(meleeDamage); // damage
							// floatEnemy
							break;
							// i = GameManager.gm.getObjects().size();
						}

					}
					if (GameManager.getObjects().get(i).getTag().equals(EntityType.largeEnemy)) {

						if (checkContactLarge(this.posX + 20, this.posY, GameManager.getObjects().get(i).getPosX(), GameManager.getObjects().get(i).getPosY())) {
							if (GameManager.getObjects().get(i).getTag().equals(EntityType.largeEnemy)) ((LargeEnemy) GameManager.getObjects().get(i)).hitMelee(meleeDamage); // damage
							// largeEnemy
							break;
							// i = GameManager.gm.getObjects().size();
						}
					}

				}
			} else if (direction == 1) {
				for (int i = 0; i < GameManager.getObjects().size(); i++) {
					if (GameManager.getObjects().get(i).getTag().equals(EntityType.turret) || GameManager.getObjects().get(i).getTag().equals(EntityType.meleeEnemy) || GameManager.getObjects().get(i).getTag().equals(EntityType.floatEnemy)) {
						if (checkContact(this.posX - 20, this.posY, GameManager.getObjects().get(i).getPosX(), GameManager.getObjects().get(i).getPosY())) {
							if (GameManager.getObjects().get(i).getTag().equals(EntityType.turret)) GameManager.getObjects().get(i).setDead(true); // kill
							// turret
							if (GameManager.getObjects().get(i).getTag().equals(EntityType.meleeEnemy)) ((MeleeEnemy) GameManager.getObjects().get(i)).hit(meleeDamage); // damage
							// meleeEnemy
							if (GameManager.getObjects().get(i).getTag().equals(EntityType.floatEnemy)) ((FloatEnemy) GameManager.getObjects().get(i)).hit(meleeDamage); // damage
							// floatEnemy
							break;
							// i = GameManager.gm.getObjects().size();
						}
					}

					if (GameManager.getObjects().get(i).getTag().equals(EntityType.largeEnemy)) {
						if (checkContactLarge(this.posX - 20, this.posY, GameManager.getObjects().get(i).getPosX(), GameManager.getObjects().get(i).getPosY())) {
							if (GameManager.getObjects().get(i).getTag().equals(EntityType.largeEnemy)) ((LargeEnemy) GameManager.getObjects().get(i)).hitMelee(meleeDamage); // damage
							// largeEnemy
							break;
							// i = GameManager.gm.getObjects().size();
						}
					}
				}
			}
		}
		// end melee

		// Beginning Left and right
		if (gc.getInput().isKey(KeyEvent.VK_D)) {

			if (GameManager.gm.getCollision(tileX + 1, tileY) || GameManager.gm.getCollision(tileX + 1, tileY + (int) Math.signum((int) offY))) {

				offX += dt * speed;
				if (offX > padding) {
					tileX += offX / GameManager.TS;
					offX = padding;
				}

			} else {
				offX += dt * speed;
			}
		}

		if (gc.getInput().isKey(KeyEvent.VK_A)) {
			if (GameManager.gm.getCollision(tileX - 1, tileY) || GameManager.gm.getCollision(tileX - 1, tileY + (int) Math.signum((int) offY))) {
				offX -= dt * speed;
				if (offX < -padding) {
					tileX += offX / GameManager.TS + 1;
					offX = -padding;
				}

			} else {
				offX -= dt * speed;
			}
		}
		// End left and right

		// Beginning Jump and Gravity

		// beginning ladder
		if (GameManager.gm.getCollisionNum((int) tileX, (int) tileY) == 5) {
			fallSpeed = 0;
			fallDistance = 0;
			ground = false;
			if (gc.getInput().isKey(KeyEvent.VK_W)) {
				fallDistance += (jump * ladderSpeed);
			}

			if (gc.getInput().isKey(KeyEvent.VK_S)) {
				fallDistance -= (jump * ladderSpeed);
			}
		}
		// end ladder

		if (gc.getInput().isKey(KeyEvent.VK_S) && !ground) {
			fallSpeed = fallSpeed * 4;
		}

		fallDistance += dt * fallSpeed;

		if ((gc.getInput().isKeyDown(KeyEvent.VK_W) || gc.getInput().isKeyDown(KeyEvent.VK_SPACE)) && ground) {
			fallDistance += jump;
			ground = false;
		}

		if (gc.getInput().isKey(KeyEvent.VK_CONTROL) && GameManager.gm.getPlayer().getMana() > GameManager.gm.getPlayer().getSlowMotionCost() && (System.nanoTime() / 1000000000.0) - lastTimeSlowUsed > slowMotionCooldown) {
			offY += fallDistance / slowMotion;
		} else {
			offY += fallDistance;

		}

		if (fallDistance < 0) {
			if ((GameManager.gm.getCollision(tileX, tileY - 1) || GameManager.gm.getCollision(tileX + (int) Math.signum((int) Math.abs(offX) > padding ? offX : 0), tileY - 1)) && offY < -paddingTop) {
				fallDistance = 0;
				offY = -paddingTop;
			}
		}

		if (fallDistance > 0) {

			if ((GameManager.gm.getCollision(tileX, tileY + 1) || GameManager.gm.getCollisionNum(tileX, tileY + 1) == 4 || GameManager.gm.getCollision(tileX + (int) Math.signum((int) Math.abs(offX) > padding ? offX : 0), tileY + 1)) && offY > 0) {
				fallDistance = 0;
				offY = 0;
				ground = true;
			}
		}

		// fallSpeed = normalFallSpeed;
		// Falling through platforms
		if (ground && gc.getInput().isKeyDown(KeyEvent.VK_S) && GameManager.gm.getCollisionNum(tileX, tileY + 1) == 4) {
			moveTo(this.posX, this.posY + 17);
		}
		// End of falling through platforms
		// End Jump and Gravity

		// teleporting
		if (gc.getInput().isKey(KeyEvent.VK_SHIFT)) {
			// teleport right
			if (direction == 0) {
				teleportX = ((tileX * GameManager.TS) + offX) + teleportRange;
				while (GameManager.gm.getCollision((int) (teleportX / GameManager.TS), (int) this.tileY) || GameManager.gm.getCollision((int) (teleportX / GameManager.TS) + 1, (int) this.tileY)) {
					teleportX--;
					if (teleportX == posX) {
						break;
					}

					if (teleportX < 0) {
						teleportX++;
						break;
					}
				}

				// teleport left
			} else {
				teleportX = ((tileX * GameManager.TS) + offX) - teleportRange;
				while (GameManager.gm.getCollision((int) (teleportX / GameManager.TS), (int) this.tileY) || GameManager.gm.getCollision((int) (teleportX / GameManager.TS) + 1, (int) this.tileY)) {
					teleportX++;
					if (teleportX == posX) {
						break;
					}

					if (teleportX > GameManager.gm.getLevelW() * GameManager.TS) {
						teleportX--;
						break;
					}
				}

			}

			teleportY = ((tileY * GameManager.TS) + offY);

			// Move teleport location up, if it collides with floor
			while (GameManager.gm.getCollision((int) (teleportX + 16) / GameManager.TS, (int) ((teleportY) / GameManager.TS) + 1)) {
				teleportY--;
				if (teleportY < 0) {
					fallDistance = 0;
					teleportY = posY;
					break;
				}
			}
		}

		if (gc.getInput().isKeyUp(KeyEvent.VK_SHIFT) && mana >= teleportCost) {
			mana -= teleportCost;
			fallDistance = 0;
			this.offX += (this.teleportX - this.posX);
			this.offY += (this.teleportY - this.posY);
			woosh.play();
		}
		// End of teleport

		if (gc.getInput().isKeyDown(KeyEvent.VK_R) && !shielded && mana >= 80) {
			shielded = true;
			mana -= 80;
		}

		// Final Position
		if (offY > GameManager.TS / 2) {
			tileY++;
			offY -= GameManager.TS;
		}

		if (offY < -GameManager.TS / 2) {
			tileY--;
			offY += GameManager.TS;
		}

		if (offX > GameManager.TS / 2) {
			tileX++;
			offX -= GameManager.TS;
		}

		if (offX < -GameManager.TS / 2) {
			tileX--;
			offX += GameManager.TS;
		}

		posX = tileX * GameManager.TS + offX;
		posY = tileY * GameManager.TS + offY;
		// end of final position

		// shooting
		if (gc.getInput().isButtonDown(1) && mana >= shootCost) { // isButton for semi auto, is buttondown for auto
			mana -= shootCost;
			GameManager.gm.addObject(new Bullet((int) (gc.getInput().getMouseX() - 16 + GameManager.gm.getCamera().getOffX()), (int) (gc.getInput().getMouseY() - 16 + GameManager.gm.getCamera().getOffY()), tileX, tileY, offX, offY));
			pew.play();
		}
		// end of shooting

		// Animation
		if (shielded) {
			shieldAnim += dt * animationSpeed;
			if (shieldAnim > 4) {
				shieldAnim = 0;
			}
		}
		if (gc.getInput().isKey(KeyEvent.VK_D)) {
			direction = 0;
			anim += dt * animationSpeed;
			if (anim > 4) {
				anim = 0;
			}

		} else if (gc.getInput().isKey(KeyEvent.VK_A)) {
			direction = 1;
			anim += dt * animationSpeed;
			if (anim > 4) {
				anim = 0;
			}
		} else {
			anim = 0;
		}

		if (!ground) {
			anim = 1;
		}

		if (ground && !groundLast) {
			anim = 2;
		}

		// End of Animation

		groundLast = ground;

		if (gc.getInput().isKeyDown(KeyEvent.VK_ESCAPE)) {
			hit(999);
		}
	}

	public void hit(int damage) {
		if (!shielded) {
			health -= damage;
			if (health < 0) {
				health = 0;
			}
			ow.play();
		} else {
			pop.play();
			shielded = false;
		}

	}

	@Override
	public void render(GameContainer gc, Renderer r) {

		// player
		if (attacking) {
			r.drawImageTile(player, (int) posX, (int) posY, (int) attackAnim, direction);
		} else {
			r.drawImageTile(player, (int) posX, (int) posY, (int) anim, direction);
		}

		// health
		r.drawFillRect((int) posX, (int) posY - 9, this.width, 4, 0xbb000000);
		r.drawFillRect((int) posX + 1, (int) posY + 1 - 9, (int) ((float) (this.width - 2) * ((float) health / (float) maxHealth)), 3, 0xbbff0000);

		// Mana
		r.drawFillRect((int) posX, (int) posY - 5, this.width, 5, 0xbb000000);
		r.drawFillRect((int) posX + 1, (int) posY + 1 - 5, (int) ((float) (this.width - 2) * ((float) mana / (float) maxMana)), 3, 0xbb00ffff);

		// ManaCost
		if (gc.getInput().isKey(KeyEvent.VK_SHIFT) && mana >= teleportCost) {
			r.drawFillRect((int) ((((int) posX + 1) + (this.width - 2) * ((float) mana / (float) maxMana)) - ((float) (this.width - 2) * ((float) teleportCost / (float) maxMana))), (int) posY + 1 - 5, (int) ((float) (this.width - 2) * ((float) teleportCost / (float) maxMana)), 3, 0xbb0000ff);
		}

		// Ghost
		if (gc.getInput().isKey(KeyEvent.VK_SHIFT) && mana >= teleportCost) {
			if (attacking) {
				r.drawImageTile(player, (int) teleportX, (int) teleportY, (int) attackAnim, direction + 2);
			} else {
				r.drawImageTile(player, (int) teleportX, (int) teleportY, (int) anim, direction + 2);
			}
		}

		// shield
		if (shielded) {
			r.drawImageTile(shield, (int) posX, (int) posY, (int) shieldAnim, 0);
		}
	}

	public void moveTo(float posX, float posY) {
		this.posX = posX;
		this.posY = posY;
		this.tileX = (int) (posX / GameManager.TS);
		this.tileY = (int) (posY / GameManager.TS);
		this.offX = (int) (posX % GameManager.TS);
		this.offY = (int) (posY % GameManager.TS);
	}

	public void setDead(boolean dead) {
		this.dead = dead;
		boof.play();
	}

	public float getMana() {
		return mana;
	}

	public void setMana(float mana) {
		this.mana = mana;
	}

	public double getSlowMotionCost() {
		return slowMotionCost;
	}

	public boolean isSlow() {
		return slow;
	}

	public int getHealth() {
		return health;
	}

	public void setHealth(int health) {
		this.health = health;
	}

	public float getTileX() {
		return tileX;
	}

	public float getTileY() {
		return tileY;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		player.dispose();
		player = null;
		ow.close();
		ow = null;
		pew.close();
		pew = null;
		woosh.close();
		woosh = null;
		boof.close();
		boof = null;
		vshh.close();
		vshh = null;
	}

}

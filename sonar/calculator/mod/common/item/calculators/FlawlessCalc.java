package sonar.calculator.mod.common.item.calculators;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import sonar.calculator.mod.Calculator;
import sonar.calculator.mod.CalculatorConfig;
import sonar.calculator.mod.common.item.calculators.CalculatorItem.CalculatorInventory;
import sonar.calculator.mod.common.tileentity.entities.EntityGrenade;
import sonar.calculator.mod.network.CalculatorGui;
import sonar.core.common.item.InventoryItem;
import sonar.core.utils.IItemInventory;
import sonar.core.utils.helpers.FontHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class FlawlessCalc extends SonarCalculator implements IItemInventory {
	
	public static class FlawlessInventory extends InventoryItem{
		public FlawlessInventory(ItemStack stack){
			super(stack,5,"FlawlessInv");
		}
	}
	public static class DynamicInventory extends InventoryItem{
		public DynamicInventory(ItemStack stack){
			super(stack,10,"DynamicInv");
		}
	}
	public static class CraftingInventory extends InventoryItem{
		public CraftingInventory(ItemStack stack){
			super(stack,10,"CraftingInv");
		}
	}

	@Override
	public InventoryItem getInventory(ItemStack stack) {	
		NBTTagCompound nbtData = stack.stackTagCompound;
		if (nbtData == null) {
			nbtData = new NBTTagCompound();
			stack.setTagCompound(nbtData);
		}
		int mode = nbtData.getInteger("Mode");
		switch(mode){
		case 0: 
			return new FlawlessInventory(stack);
		case 1:
			return new DynamicInventory(stack);
		case 2:
			return new CraftingInventory(stack);
		}
		return null;
	}
	
	public FlawlessCalc() {
		capacity = CalculatorConfig.flawlessEnergy;
		maxReceive = CalculatorConfig.flawlessEnergy;
		maxExtract = CalculatorConfig.flawlessEnergy;
		maxTransfer = CalculatorConfig.flawlessEnergy;
	}

	private static final int FlawlessCraft = 0;
	private static final int DynamicCraft = 1;
	private static final int Crafting = 2;
	private static final int Grenade = 3;
	private static final int Ender = 4;
	private static final int Teleport = 5;

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List list,
			boolean par4) {
		super.addInformation(stack, player, list, par4);

		NBTTagCompound nbtData = stack.stackTagCompound;
		if (nbtData == null) {
			nbtData = new NBTTagCompound();
			stack.setTagCompound(nbtData);
		}
		list.add(StatCollector.translateToLocal(
				"calc.mode")+": " + chat(stack, player));

		int storedItems = new FlawlessInventory(stack).getItemsStored(stack)
				+new DynamicInventory(stack).getItemsStored(stack)
				+ new CraftingInventory(stack).getItemsStored(stack);
		if(storedItems!=0){
			list.add(StatCollector.translateToLocal("calc.storedstacks")+": " + storedItems);}
	}
	

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world,
			EntityPlayer player) {
			NBTTagCompound nbtData = stack.stackTagCompound;
			if (nbtData == null) {
				nbtData = new NBTTagCompound();
				stack.setTagCompound(nbtData);
			}
			int mode = nbtData.getInteger("Mode");
			if (player.isSneaking()) {
				mode = (mode + 1) % 6;
				nbtData.setInteger("Mode", mode);
				nbtData.setBoolean("Grenade", false);
				if(!world.isRemote){
				FontHelper.sendMessage(chat(stack, player), world, player);}
			} else {
				switch (mode) {
				case FlawlessCraft:
					player.openGui(Calculator.instance,
							CalculatorGui.FlawlessCalculator, world,
							(int) player.posX, (int) player.posY,
							(int) player.posZ);

					break;
				case DynamicCraft:
					player.openGui(Calculator.instance,
							CalculatorGui.PortableDynamic, world,
							(int) player.posX, (int) player.posY,
							(int) player.posZ);

					break;
				case Crafting:
					player.openGui(Calculator.instance,
							CalculatorGui.PortableCrafting, world,
							(int) player.posX, (int) player.posY,
							(int) player.posZ);
					break;

				case Grenade:

					if (CalculatorConfig.enableGrenades) {
					if (!world.isRemote && player.capabilities.isCreativeMode) {
						explosion(stack,world, player);

					} else if (getEnergyStored(stack) >= 1000) {
						explosion(stack,world, player);
						this.extractEnergy(stack, 1000, false);
					} else if ((getEnergyStored(stack) <= 1000)
							&& (!world.isRemote)) {
						player.addChatComponentMessage(new ChatComponentText(
								StatCollector.translateToLocal("energy.notEnough")));
					}
					}else{
						FontHelper.sendMessage(StatCollector.translateToLocal("calc.ban"), world, player);
					}
					break;
				case Ender:
					if (player.capabilities.isCreativeMode) {
						ender(world, player);

					} else if (getEnergyStored(stack) >= 1000) {
						ender(world, player);
						this.extractEnergy(stack, 1000, false);
					} else if ((getEnergyStored(stack) <= 1000)
							&& (!world.isRemote)) {
						player.addChatComponentMessage(new ChatComponentText(
								StatCollector.translateToLocal("energy.notEnough")));
					}

					break;

				case Teleport:
					teleport(player, world, stack);
					break;
				}

			}
			return super.onItemRightClick(stack, world, player);
		}

	

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world,
			int x, int y, int z, int par7, float par8, float par9, float par10) {

		if (!(world.getBlock(x, y, z) == Calculator.stablestoneBlock)) {
			return false;
		}
		int mode = stack.stackTagCompound.getInteger("Mode");
		if (world.getBlock(x, y, z) == Calculator.stablestoneBlock) {
			if (stack.hasTagCompound()) {
				if (mode == FlawlessCalc.Teleport) {
					if ((world.getBlock(x, y, z) == Calculator.stablestoneBlock)
							&& (!stack.stackTagCompound.getBoolean("Tele"))) {
						stack.stackTagCompound.setDouble("TeleX", x);
						stack.stackTagCompound.setDouble("TeleY", y + 1);
						stack.stackTagCompound.setDouble("TeleZ", z);
						stack.stackTagCompound.setBoolean("Tele", true);
						stack.stackTagCompound.setInteger("Dimension",
								player.dimension);
						if (!world.isRemote) {
							player.addChatComponentMessage(new ChatComponentText(

									StatCollector.translateToLocal("calc.position")));
						}

					} else if ((world.getBlock(x, y, z) == Calculator.stablestoneBlock)
							&& (stack.stackTagCompound.getBoolean("Tele"))
							&& (world.getBlock((int) stack.stackTagCompound
									.getDouble("TeleX"),
									(int) stack.stackTagCompound
											.getDouble("TeleY") - 1,
									(int) stack.stackTagCompound
											.getDouble("TeleZ")) != Calculator.stablestoneBlock)) {
						stack.stackTagCompound.setDouble("TeleX", x);
						stack.stackTagCompound.setDouble("TeleY", y + 1);
						stack.stackTagCompound.setDouble("TeleZ", z);
						stack.stackTagCompound.setBoolean("Tele", true);
						stack.stackTagCompound.setInteger("Dimension",
								player.dimension);
						if (!world.isRemote) {
							player.addChatComponentMessage(new ChatComponentText(
									StatCollector.translateToLocal("calc.position")));
						}

					}
				}

			}
		}
		return false;
	}

	public void teleport(EntityPlayer player, World world, ItemStack stack) {
		if (player.dimension == stack.stackTagCompound.getInteger("Dimension")) {
			if (stack.stackTagCompound.getBoolean("Tele")) {
				if (world.getBlock(
						(int) stack.stackTagCompound.getDouble("TeleX"),
						(int) stack.stackTagCompound.getDouble("TeleY") - 1,
						(int) stack.stackTagCompound.getDouble("TeleZ")) == Calculator.stablestoneBlock) {
					player.setPositionAndUpdate(
							stack.stackTagCompound.getDouble("TeleX"),
							stack.stackTagCompound.getDouble("TeleY"),
							stack.stackTagCompound.getDouble("TeleZ"));

				}

				if ((world.getBlock(
						(int) stack.stackTagCompound.getDouble("TeleX"),
						(int) stack.stackTagCompound.getDouble("TeleY") - 1,
						(int) stack.stackTagCompound.getDouble("TeleZ")) != Calculator.stablestoneBlock)
						&& (!world.isRemote)) {
					player.addChatComponentMessage(new ChatComponentText(

							StatCollector.translateToLocal("calc.stableStone")));
				}

			} else if ((!stack.stackTagCompound.getBoolean("Tele"))
					&& (!world.isRemote)) {
				player.addChatComponentMessage(new ChatComponentText(
						StatCollector.translateToLocal("calc.noPosition")));
			}
		}

		if (player.dimension != stack.stackTagCompound.getInteger("Dimension")) {
			if (!world.isRemote) {
				player.addChatComponentMessage(new ChatComponentText(
						StatCollector.translateToLocal("calc.dimension")));
			}
		}
	}

	public int currentEnergy(ItemStack itemstack) {
		return getEnergyStored(itemstack);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister iconregister) {
		this.itemIcon = iconregister
				.registerIcon("Calculator:flawlesscalculator");
	}

	public void ender(World world, EntityPlayer player) {
		world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F);
		if (!world.isRemote) {
			world.spawnEntityInWorld(new EntityEnderPearl(world, player));
		}
	}

	public void explosion(ItemStack stack, World world, EntityPlayer player) {
		if(stack.stackTagCompound.getBoolean("Grenade")){
		world.playSoundAtEntity(player, "random.fizz", 0.7F, 0.8F);

		if (!world.isRemote) {
			world.spawnEntityInWorld(new EntityGrenade(world, player));
		}
		stack.stackTagCompound.setBoolean("Grenade", false);
		}else{
			if (!world.isRemote) {
				player.addChatComponentMessage(new ChatComponentText(
						StatCollector.translateToLocal("calc.grenade")));
			}	
			stack.stackTagCompound.setBoolean("Grenade", true);
		}
		
	}

	public String chat(ItemStack stack, EntityPlayer player) {
		NBTTagCompound nbtData = stack.stackTagCompound;

		int current = nbtData.getInteger("Mode");
		if (current == FlawlessCalc.FlawlessCraft) {
			return StatCollector.translateToLocal("flawless.mode1");
		}
		if (current == FlawlessCalc.DynamicCraft) {
			return StatCollector.translateToLocal("flawless.mode2");
		}
		if (current == FlawlessCalc.Crafting) {
			return StatCollector.translateToLocal("flawless.mode3");
		}
		if (current == FlawlessCalc.Grenade) {
			return StatCollector.translateToLocal("flawless.mode4");
		}
		if (current == FlawlessCalc.Ender) {
			return StatCollector.translateToLocal("flawless.mode5");
		}
		if (current == FlawlessCalc.Teleport) {
			return StatCollector.translateToLocal("flawless.mode6");
		}
		return StatCollector.translateToLocal("flawless.mode");
	}

	@Override
	public boolean hasEffect(ItemStack stack) {
		return true;
	}
	
	public int getMode(ItemStack stack){
		NBTTagCompound nbtData = stack.stackTagCompound;
		if (nbtData == null) {
			nbtData = new NBTTagCompound();
			stack.setTagCompound(nbtData);
		}
		return nbtData.getInteger("Mode");
		
	}

}
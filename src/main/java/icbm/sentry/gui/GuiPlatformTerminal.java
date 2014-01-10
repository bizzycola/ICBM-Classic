package icbm.sentry.gui;

import icbm.sentry.platform.TileEntityTurretPlatform;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.InventoryPlayer;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import calclavia.lib.terminal.TileTerminal;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dark.lib.prefab.invgui.ContainerFake;

/** The console GUI used by turrets.
 * 
 * @author Darkguardsman, Edited by Calclavia */
@SideOnly(Side.CLIENT)
public class GuiPlatformTerminal extends GuiPlatformBase
{
    private GuiTextField commandLine;

    public GuiPlatformTerminal(InventoryPlayer entityPlayer, TileEntityTurretPlatform tileEntity)
    {
        super(entityPlayer, new ContainerFake(tileEntity), tileEntity);
        this.tileEntity = tileEntity;
    }

    @Override
    public void initGui()
    {
        super.initGui();
        int width = (this.width - this.xSize) / 2;
        int height = (this.height - this.ySize) / 2;

        this.commandLine = new GuiTextField(this.fontRenderer, width + 12, height + 165, 135, 11);
        this.commandLine.setMaxStringLength(30);

        this.buttonList.add(new GuiButtonArrow(MAX_BUTTON_ID + 1, width + 151, height + 21, false));
        this.buttonList.add(new GuiButtonArrow(MAX_BUTTON_ID + 2, width + 151, height + 152, true));
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        this.commandLine.setFocused(true);
    }

    @Override
    public void handleMouseInput()
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel > 0)
        {
            ((TileTerminal) this.tileEntity).scroll(-2);
        }
        else if (wheel < 0)
        {
            ((TileTerminal) this.tileEntity).scroll(2);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        super.actionPerformed(button);

        switch (button.id)
        {
            case MAX_BUTTON_ID + 1:
            {
                // Arrow Up
                ((TileTerminal) this.tileEntity).scroll(-1);
                break;
            }
            case MAX_BUTTON_ID + 2:
            {
                // Arrow Down
                ((TileTerminal) this.tileEntity).scroll(1);
                break;
            }
        }
    }

    @Override
    protected void keyTyped(char character, int keycode)
    {
        if (keycode == Keyboard.KEY_ESCAPE)
        {
            this.mc.thePlayer.closeScreen();
        }
        else if (keycode == 200) // PAGE UP (no constant)
        {
            ((TileTerminal) this.tileEntity).scroll(-1);
        }
        else if (keycode == 208) // PAGE DOWN (no constant)
        {
            ((TileTerminal) this.tileEntity).scroll(1);
        }
        else if (keycode == Keyboard.KEY_RETURN)
        {
            ((TileTerminal) this.tileEntity).sendCommandToServer(this.entityPlayer, this.commandLine.getText());
            this.commandLine.setText("");
        }
        else
        {
            this.commandLine.textboxKeyTyped(character, keycode);
        }
    }

    @Override
    protected void mouseClicked(int par1, int par2, int par3)
    {
        super.mouseClicked(par1, par2, par3);
        this.commandLine.mouseClicked(par1, par2, par3);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y)
    {
        String title = "Terminal";
        this.fontRenderer.drawString("\u00a77" + title, (int) (this.xSize / 2 - title.length() * 2.5), 4, 4210752);
        this.drawConsole(25, 16, TileTerminal.SCROLL_SIZE);
        super.drawGuiContainerForegroundLayer(x, y);
    }

    public void drawConsole(int x, int y, int lines)
    {
        int spacing = 10;
        int color = 14737632;

        GL11.glPushMatrix();
        float scale = 0.92f;
        GL11.glScalef(scale, scale, scale);

        // Draws each line
        for (int i = 0; i < lines; i++)
        {
            int currentLine = i + ((TileTerminal) this.tileEntity).getScroll();

            if (currentLine < ((TileTerminal) this.tileEntity).getTerminalOuput().size() && currentLine >= 0)
            {
                String line = ((TileTerminal) this.tileEntity).getTerminalOuput().get(currentLine);

                if (line != null && line != "")
                {
                    this.fontRenderer.drawString(line, y, spacing * i + x, color);
                }
            }
        }

        GL11.glPopMatrix();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int x, int y)
    {
        super.drawGuiContainerBackgroundLayer(par1, x, y);
        this.commandLine.drawTextBox();
    }
}
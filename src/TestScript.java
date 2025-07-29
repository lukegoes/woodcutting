import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.input.Keyboard;
import org.dreambot.api.input.Mouse;

import javax.swing.*;

@ScriptManifest(name = "Woodcutting", description = "Terceira Tentativa", author = "Luk",
        version = 1.65, category = Category.WOODCUTTING, image = "")
public class TestScript extends AbstractScript {

    State state;
    Area treeArea;
    Area bankArea;
    String treeName = "Tree";
    private String logName;
    boolean isPowerMining = true;

    @Override
    public void onStart() {
        Logger.log("Script iniciado: Woodcutting by Luk");

        final JFrame frame = new JFrame("Make your choices!");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(300, 220);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Árvore
        JLabel treeLabel = new JLabel("Qual árvore deseja cortar?");
        JRadioButton normalTreeButton = new JRadioButton("🌳 Normal Tree (nível 1–15)");
        JRadioButton oakTreeButton = new JRadioButton("🌳 Oak Tree (nível 15–30)");
        ButtonGroup treeGroup = new ButtonGroup();
        treeGroup.add(normalTreeButton);
        treeGroup.add(oakTreeButton);

        // Modo
        JLabel methodLabel = new JLabel("Modo de corte:");
        JRadioButton powerButton = new JRadioButton("⚡ Power Cutting (drop logs)");
        JRadioButton bankingButton = new JRadioButton("🏦 Banking (guardar logs no banco)");
        ButtonGroup methodGroup = new ButtonGroup();
        methodGroup.add(powerButton);
        methodGroup.add(bankingButton);

        JButton startButton = new JButton("Iniciar");

        // Adiciona ao painel
        panel.add(treeLabel);
        panel.add(normalTreeButton);
        panel.add(oakTreeButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(methodLabel);
        panel.add(powerButton);
        panel.add(bankingButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(startButton);

        frame.add(panel);
        frame.setVisible(true);

        // Ação do botão iniciar
        startButton.addActionListener(e -> {
            if (normalTreeButton.isSelected()) {
                Logger.log("Selecionado: Normal Tree");
                treeName = "Tree";
                treeArea = new Area(3149, 3465, 3171, 3449);
                bankArea = new Area(3179, 3448, 3191, 3432);
                logName = "Logs";
            } else if (oakTreeButton.isSelected()) {
                Logger.log("Selecionado: Oak Tree");
                treeName = "Oak Tree";
                treeArea = new Area(3098, 3248, 3103, 3238);
                bankArea = new Area(3090, 3245, 3096, 3240);
                logName = "Oak logs";
            }

            if (powerButton.isSelected()) {
                isPowerMining = true;
                Logger.log("Modo selecionado: Power Cutting");
            } else if (bankingButton.isSelected()) {
                isPowerMining = false;
                Logger.log("Modo selecionado: Banking");
            }

            frame.dispose();
        });

        while (frame.isVisible()) {
            Sleep.sleep(100);
        }
    }

    @Override
    public int onLoop() {
        State currentState = getState();
        Logger.log("Estado atual: " + currentState);
        switch (getState()) {
            case WALKING_TO_BANK:
                Logger.log("Ação: Andando até o banco.");
                if (!Players.getLocal().isMoving()) {
                    Logger.log("Jogador parado, iniciando caminhada até o banco.");
                    Walking.walk(bankArea.getRandomTile());
                }
                break;
            case USE_BANK:
                Logger.log("Ação: Tentando abrir o banco.");
                if (!Bank.isOpen()) {
                    GameObject bankBooth = GameObjects.closest("Bank booth");
                    if (bankBooth != null) {
                        Logger.log("Interagindo com o banco...");
                        bankBooth.interact("Bank");
                        Sleep.sleepUntil(() -> Bank.isOpen(), 4000);
                    } else {
                        Logger.log("Bank booth não encontrado.");
                    }
                }
                else {
                    Logger.log("Banco já aberto, preparando para depositar.");
                }
                break;
            case BANKING:
                Logger.log("Ação: Depositando " + logName);
                Bank.depositAll(logName);
                Sleep.sleepUntil(() -> !Inventory.contains(logName), 2000);
                if (!Inventory.contains(logName)) {
                    Logger.log("Depósito concluído, fechando o banco.");
                    Bank.close();
                }else {
                    Logger.log(logName + " ainda está no inventário.");
                }
                break;
            case WALKING_TO_TREES:
                Logger.log("Ação: Andando até as árvores.");
                if (!Players.getLocal().isMoving()) {
                    Logger.log("Jogador parado, iniciando caminhada até a área de árvores.");
                    Walking.walk(treeArea.getRandomTile());
                }
                break;
            case FINDING_TREE:
                Logger.log("Ação: Procurando uma árvore para cortar.");
                if (!Players.getLocal().isAnimating() && !Players.getLocal().isMoving()) {
                    GameObject tree = GameObjects.closest(t ->
                            t.getName().equalsIgnoreCase(treeName) && treeArea.contains(t.getTile())
                    );
                    Logger.log("Árvore encontrada, tentando cortar.");
                    if (tree != null && tree.interact("Chop down")) {
                        Logger.log("Interagindo com a árvore...");
                        Sleep.sleepUntil(() -> Players.getLocal().isAnimating(), 4000);
                    }
                } else {
                    Logger.log("Nenhuma árvore encontrada.");
                }
                break;
            case DROPPING_LOGS:
                Logger.log("Ação: Dropando " +logName);
                dropItemsByName(logName);
                break;
            case CHOPPING_TREE:
                Logger.log("Ação: Cortando árvore.");
                //put some random actions here for antibot
                break;
        }
        return Calculations.random(300, 700);
    }

    private State getState() {
        if (isPowerMining && Inventory.isFull()) {
            Logger.log("Condição: Inventário cheio e modo Power Mining ativo.");
            return State.DROPPING_LOGS;
        }
        if (Inventory.isFull() && !bankArea.contains(Players.getLocal().getTile())) {
            Logger.log("Condição: Inventário cheio e longe do banco.");
            return State.WALKING_TO_BANK;
        }
        else if (Inventory.isFull() && bankArea.contains(Players.getLocal().getTile()) && !Bank.isOpen()) {
            Logger.log("Condição: Inventário cheio e no banco.");
            return State.USE_BANK;
        }
        else if (Bank.isOpen() && Inventory.isFull()) {
            Logger.log("Condição: Banco aberto e inventário cheio.");
            return State.BANKING;
        }
        else if (!Inventory.isFull() && !treeArea.contains(Players.getLocal().getTile())) {
            Logger.log("Condição: Inventário não está cheio e jogador está longe das árvores.");
            return State.WALKING_TO_TREES;
        }
        else if (!Inventory.isFull() && !Players.getLocal().isAnimating() && treeArea.contains(Players.getLocal().getTile())) {
            Logger.log("Condição: Pronto para procurar árvore.");
            return State.FINDING_TREE;
        } else if (!Inventory.isFull() && Players.getLocal().isAnimating() && treeArea.contains(Players.getLocal().getTile())) {
            Logger.log("Condição: Atualmente cortando árvore.");
            return State.CHOPPING_TREE;
        }
        return state;
    }

    public void dropItemsByName(String itemName) {
        if (itemName == null) {
            Logger.log("Nome do item não definido. Cancelando drop.");
            return;
        }

        Logger.log("Dropando itens com nome: " + itemName);

        Keyboard.pressShift();

        for (int i = 0; i < 28; i++) {
            Item item = Inventory.getItemInSlot(i);
            if (item != null && item.getName().equalsIgnoreCase(itemName)) {
                Mouse.click(Inventory.slotBounds(i));
                Sleep.sleep(50, 100);
            }
        }

        Keyboard.releaseShift();
    }

}


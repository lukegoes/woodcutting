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
import java.awt.Point;
import java.util.List;
import static org.dreambot.api.methods.input.Camera.mouseRotateToYaw;
import java.util.HashMap;
import java.util.Map;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;


@ScriptManifest(name = "Woodcutting", description = "Terceira Tentativa", author = "Luk",
        version = 1.9, category = Category.WOODCUTTING, image = "")
public class TestScript extends AbstractScript {

    State state;
    Area treeArea;
    Area bankArea;
    String treeName = "Tree";
    private String logName;
    private String modeName;
    private State lastLoggedState = null;
    private Map<String, String> lastLogs = new HashMap<>();
    private long startTime;
    private int logsCortados = 0;
    private int lastInventoryCount = 0;
    private String ultimaAcao = "Iniciando...";


    boolean isPowerMining = true;

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        logCategory("start","Script iniciado: Woodcutting by Luk");

        final JFrame frame = new JFrame("Make your choices!");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(300, 220);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Árvore
        JLabel treeLabel = new JLabel("Qual árvore deseja cortar?");
        JRadioButton normalTreeButton = new JRadioButton("Normal Tree");
        JRadioButton oakTreeButton = new JRadioButton("Oak Tree");
        JRadioButton willowTreeButton = new JRadioButton("Willow Tree");
        ButtonGroup treeGroup = new ButtonGroup();
        treeGroup.add(normalTreeButton);
        treeGroup.add(oakTreeButton);

        // Modo
        JLabel methodLabel = new JLabel("Modo de corte:");
        JRadioButton powerButton = new JRadioButton("Power Cutting");
        JRadioButton bankingButton = new JRadioButton("Banking");
        ButtonGroup methodGroup = new ButtonGroup();
        methodGroup.add(powerButton);
        methodGroup.add(bankingButton);

        JButton startButton = new JButton("Iniciar");

        // Adiciona ao painel
        panel.add(treeLabel);
        panel.add(normalTreeButton);
        panel.add(oakTreeButton);
        panel.add(willowTreeButton);
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
                logCategory("árvore","Selecionado: Normal Tree");
                treeName = "Tree";
                treeArea = new Area(3149, 3465, 3171, 3449);
                bankArea = new Area(3179, 3448, 3191, 3432);
                logName = "Logs";
            } else if (oakTreeButton.isSelected()) {
                logCategory("árvore","Selecionado: Oak Tree");
                treeName = "Oak Tree";
                treeArea = new Area(3098, 3248, 3103, 3238);
                bankArea = new Area(3090, 3245, 3096, 3240);
                logName = "Oak logs";
            } else if (willowTreeButton.isSelected()) {
                logCategory("árvore","Selecionado: Willow Tree");
                treeName = "Willow Tree";
                treeArea = new Area(3081, 3240, 3093, 3225);
                bankArea = new Area(3090, 3245, 3096, 3240);

                logName = "Willow logs";
            }

            if (powerButton.isSelected()) {
                isPowerMining = true;
                modeName = "Power Training";
                logCategory("modo","Modo selecionado: Power Cutting");
            } else if (bankingButton.isSelected()) {
                isPowerMining = false;
                modeName = "Banking";
                logCategory("modo","Modo selecionado: Banking");
            }

            frame.dispose();
        });

        while (frame.isVisible()) {
            Sleep.sleep(100);
        }
    }

    @Override
    public int onLoop() {
        int currentCount = Inventory.count(logName);
        if (currentCount > lastInventoryCount) {
            logsCortados += (currentCount - lastInventoryCount);
        }
        lastInventoryCount = currentCount;
        State currentState = getState();
        if (currentState != lastLoggedState) {
            logCategory("estado", "Mudança de estado: " + currentState);
            lastLoggedState = currentState;
            lastLogs.clear();
        }
        switch (currentState) {
            case WALKING_TO_BANK:
                logCategory("ação", "Andando até o banco.");
                if (!Players.getLocal().isMoving()) {
                    logCategory("estado","Jogador parado, iniciando caminhada até o banco.");
                    Walking.walk(bankArea.getRandomTile());
                }
                break;
            case USE_BANK:
                logCategory("ação","Tentando abrir o banco.");
                if (!Bank.isOpen()) {
                    GameObject bankBooth = GameObjects.closest("Bank booth");
                    if (bankBooth != null) {
                        logCategory("ação","Interagindo com o banco...");
                        bankBooth.interact("Bank");
                        Sleep.sleepUntil(() -> Bank.isOpen(), 4000);
                    } else {
                        logCategory("estado","Bank booth não encontrado.");
                    }
                }
                else {
                    logCategory("estado","Banco já aberto, preparando para depositar.");
                }
                break;
            case BANKING:
                logCategory("ação","Depositando " + logName);
                Bank.depositAll(logName);
                Sleep.sleepUntil(() -> !Inventory.contains(logName), 2000);
                if (!Inventory.contains(logName)) {
                    logCategory("estado","Depósito concluído, fechando o banco.");
                    Bank.close();
                }else {
                    logCategory("estado",logName + " ainda está no inventário.");
                }
                break;
            case WALKING_TO_TREES:
                logCategory("ação","Andando até as árvores.");
                if (!Players.getLocal().isMoving()) {
                    logCategory("estado","Jogador parado, iniciando caminhada até a área de árvores.");
                    Walking.walk(treeArea.getRandomTile());
                }
                break;
            case FINDING_TREE:
                logCategory("ação","Procurando uma árvore para cortar.");
                if (!Players.getLocal().isAnimating() && !Players.getLocal().isMoving()) {
                    GameObject tree = GameObjects.closest(t ->
                            t.getName().equalsIgnoreCase(treeName) && treeArea.contains(t.getTile())
                    );
                    logCategory("estado","Árvore encontrada, tentando cortar.");
                    if (tree != null && tree.interact("Chop down")) {
                        logCategory("ação","Interagindo com a árvore...");
                        Sleep.sleepUntil(() -> Players.getLocal().isAnimating(), 4000);
                    }
                    if (Calculations.random(1, 100) <= 10) {
                        performAntiban();
                    }
                } else {
                    logCategory("estado","Nenhuma árvore encontrada.");
                }
                break;
            case DROPPING_LOGS:
                logCategory("ação","Dropando " +logName);
                dropItemsByName(logName);
                break;
            case CHOPPING_TREE:
                logCategory("ação","Cortando árvore.");
                if (Calculations.random(1, 100) <= 15) {
                    performAntiban(); 
                }
                break;
        }
        return randomDelay(300, 700, Calculations.random(100, 200));
    }

    private State getState() {
        if (isPowerMining && Inventory.isFull()) {
            logCategory("condição","Inventário cheio e modo Power Mining ativo.");
            return State.DROPPING_LOGS;
        }
        if (Inventory.isFull() && !bankArea.contains(Players.getLocal().getTile())) {
            logCategory("condição","Inventário cheio e longe do banco.");
            return State.WALKING_TO_BANK;
        }
        else if (Inventory.isFull() && bankArea.contains(Players.getLocal().getTile()) && !Bank.isOpen()) {
            logCategory("condição", "Inventário cheio e no banco.");
            return State.USE_BANK;
        }
        else if (Bank.isOpen() && Inventory.isFull()) {
            logCategory("condição","Banco aberto e inventário cheio.");
            return State.BANKING;
        }
        else if (!Inventory.isFull() && !treeArea.contains(Players.getLocal().getTile())) {
            logCategory("condição","Inventário não está cheio e jogador está longe das árvores.");
            return State.WALKING_TO_TREES;
        }
        else if (!Inventory.isFull() && !Players.getLocal().isAnimating() && treeArea.contains(Players.getLocal().getTile())) {
            logCategory("condição","Pronto para procurar árvore.");
            return State.FINDING_TREE;
        } else if (!Inventory.isFull() && Players.getLocal().isAnimating() && treeArea.contains(Players.getLocal().getTile())) {
            logCategory("condição","Atualmente cortando árvore.");
            return State.CHOPPING_TREE;
        }
        return state;
    }

    public void dropItemsByName(String itemName) {
        if (itemName == null) {
            logCategory("estado","Nome do item não definido. Cancelando drop.");
            return;
        }

        logCategory("ação","Dropando itens com nome: " + itemName);

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

    public int randomDelay(int min, int max, double dev) {
        double mean = (min + max) / 2.0;
        double gaussian = Calculations.nextGaussianRandom(mean, dev);
        return (int) Math.max(min, Math.min(max, gaussian));
    }

    private void performAntiban() {
        int chance = Calculations.random(1, 100);

        if (chance <= 10) {
            logCategory("antiban","Movimentando o mouse aleatoriamente.");
            Mouse.moveOutsideScreen();
            Sleep.sleep(Calculations.random(1000, 3000));
        } else if (chance <= 20) {
            logCategory("antiban","Movendo mouse para área da aba de skills.");
            int x = (int) Calculations.nextGaussianRandom(550, 20);
            int y = (int) Calculations.nextGaussianRandom(190, 20);
            Mouse.move(new Point(x, y));
            Sleep.sleep(Calculations.random(800, 1600));
        } else if (chance <= 25) {
            logCategory("antiban","Inspecionando objeto aleatório.");

            // Pega todos os objetos carregados no mapa
            List<GameObject> objetos = GameObjects.all();

            // Filtra para garantir que tenha objetos visíveis e interagíveis
            objetos = objetos.stream()
                    .filter(obj -> obj != null && obj.exists() && obj.hasAction("Examine"))
                    .toList();

            if (!objetos.isEmpty()) {
                // Escolhe um objeto aleatório
                GameObject aleatorio = objetos.get(Calculations.random(0, objetos.size() - 1));

                logCategory("antiban","Examinando: " + aleatorio.getName());
                aleatorio.interact("Examine");
                Sleep.sleep(Calculations.random(1000, 2000));
            }
        } else if (chance <= 40) {
            logCategory("antiban","Girando a câmera.");
            mouseRotateToYaw(Calculations.random(0, 360));
            Sleep.sleep(Calculations.random(800, 1500));
        }

    }

    private void logCategory(String category, String message) {
        String last = lastLogs.get(category);
        if (!message.equals(last)) {
            Logger.log(message);
            lastLogs.put(category, message);
            if (category.equals("ação")) {
                ultimaAcao = message;
            }
        }
    }

    @Override
    public void onPaint(Graphics g) {
        // Configurações visuais
        g.setColor(new Color(0, 0, 0, 160)); // fundo semitransparente
        g.fillRoundRect(10, 20, 250, 100, 10, 10);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Poppins", Font.PLAIN, 12));

        // Info
        g.drawString("Tempo rodando: " + formatTime(System.currentTimeMillis() - startTime), 20, 50);
        g.drawString("Modo: " + logName + " + " + modeName, 20, 70);
        g.drawString("Última ação: " + ultimaAcao, 20, 90);
        g.drawString("Logs cortados: " + logsCortados, 20, 110);
    }

    private String formatTime(long time) {
        long seconds = time / 1000 % 60;
        long minutes = time / (1000 * 60) % 60;
        long hours = time / (1000 * 60 * 60);
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }



}


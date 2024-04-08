/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.majadamarcial.chatproyect;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 *
 * @author Jose
 */
public class Cliente {

    public static void main(String[] args) {
        MarcoCliente miMarco = new MarcoCliente();
        miMarco.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}

class MarcoCliente extends JFrame implements ActionListener, Runnable {

    private JPanel lamina;
    private String pideNick, ipServer;
    private int puertoServer, puertoCliente;
    private JLabel titulo, miNick, nick;
    private JComboBox listaUsu;
    private Items lu;
    private JTextArea mensajes, campoMensaje;
    private JScrollPane scrollMensajes, scrollMensaje;
    private JButton botonEnvio;
    private Thread miHilo;
    private Socket miSocket, cliente;
    private ServerSocket servidorCliente;
    private PaqueteEnvios datos, paqueteRecibido;
    private ObjectOutputStream paqueteDatosEnvio;
    private ObjectInputStream paqueteDatosRecibo;

    public MarcoCliente() {
        ipServer = "192.168.8.100";
        puertoServer = 9999;
        puertoCliente = 9090;

        setBounds(10, 10, 900, 480);
        pideNick = JOptionPane.showInputDialog("Introduce tu Nick: ");
        lamina = new JPanel();
        nick = new JLabel("Nick: ");
        lamina.add(nick);
        miNick = new JLabel(pideNick);
        lamina.add(miNick);
        titulo = new JLabel("################################################################# CHAT #################################################################");
        lamina.add(titulo);
        mensajes = new JTextArea(17, 80);
        mensajes.setEditable(false);
        scrollMensajes = new JScrollPane(mensajes);
        lamina.add(scrollMensajes);
        campoMensaje = new JTextArea(5, 50);
        scrollMensaje = new JScrollPane(campoMensaje);
        lamina.add(scrollMensaje);
        listaUsu = new JComboBox();
        listaUsu.addItem(new Items("Todos", ""));
        lamina.add(listaUsu);
        botonEnvio = new JButton("Enviar");
        botonEnvio.addActionListener(this);
        lamina.add(botonEnvio);
        add(lamina);
        setVisible(true);
        addWindowListener(new ConDescon(miNick.getText(), ipServer, puertoServer));
        miHilo = new Thread(this);
        miHilo.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            miSocket = new Socket(ipServer, puertoServer);
            lu = (Items) listaUsu.getSelectedItem();
            if (!listaUsu.getSelectedItem().toString().equals("Todos")) {
                mensajes.append(miNick.getText() + " -> " + lu.getValue() + ": " + campoMensaje.getText() + "\n");
            }
            datos = new PaqueteEnvios();
            datos.setNick(miNick.getText());
            datos.setDestino(lu);
            datos.setMensaje(campoMensaje.getText());

            paqueteDatosEnvio = new ObjectOutputStream(miSocket.getOutputStream());
            paqueteDatosEnvio.writeObject(datos);
            paqueteDatosEnvio.close();

            campoMensaje.setText("");
        } catch (IOException ex) {
            Logger.getLogger(MarcoCliente.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            servidorCliente = new ServerSocket(puertoCliente);

            while (true) {
                cliente = servidorCliente.accept();
                paqueteDatosRecibo = new ObjectInputStream(cliente.getInputStream());
                paqueteRecibido = (PaqueteEnvios) paqueteDatosRecibo.readObject();
                mensajes.append(paqueteRecibido.getMensaje() + "\n");
                if (paqueteRecibido.getConectando() || paqueteRecibido.getDesconectando()) {
                    listaUsu.removeAllItems();
                    listaUsu.addItem(new Items("Todos", ""));
                    paqueteRecibido.getConectados().entrySet().forEach(d -> {
                        listaUsu.addItem(new Items(d.getKey(), d.getValue()));
                    });
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MarcoCliente.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(MarcoCliente.class
                    .getName()).log(Level.SEVERE, null, ex);

        }
    }
}

class ConDescon extends WindowAdapter {

    private Socket miSocket;
    private PaqueteEnvios datos;
    private ObjectOutputStream paqueteDatos;
    private String miNick, ipServer;
    private int puertoServer;

    public ConDescon(String nick, String ip, int puerto) {
        miNick = nick;
        ipServer = ip;
        puertoServer = puerto;
    }

    public void windowOpened(WindowEvent e) {
        try {
            miSocket = new Socket(ipServer, puertoServer);
            datos = new PaqueteEnvios();
            datos.setConectando(true);
            datos.setNick(miNick);
            paqueteDatos = new ObjectOutputStream(miSocket.getOutputStream());
            paqueteDatos.writeObject(datos);
            paqueteDatos.close();
        } catch (IOException ex) {
            Logger.getLogger(ConDescon.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void windowClosing(WindowEvent e) {
        try {
            miSocket = new Socket(ipServer, puertoServer);
            datos = new PaqueteEnvios();
            datos.setDesconectando(true);
            datos.setNick(miNick);
            paqueteDatos = new ObjectOutputStream(miSocket.getOutputStream());
            paqueteDatos.writeObject(datos);
            paqueteDatos.close();
        } catch (IOException ex) {
            Logger.getLogger(ConDescon.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}




class PaqueteEnvios implements Serializable {

    private String nick, mensaje;
    private Items destino;
    private Boolean conectando = false, desconectando = false;
    private Map<String, String> conectados;

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public Items getDestino() {
        return destino;
    }

    public void setDestino(Items destino) {
        this.destino = destino;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Boolean getConectando() {
        return conectando;
    }

    public void setConectando(Boolean conectando) {
        this.conectando = conectando;
    }

    public Boolean getDesconectando() {
        return desconectando;
    }

    public void setDesconectando(Boolean desconectando) {
        this.desconectando = desconectando;
    }

    public Map<String, String> getConectados() {
        return conectados;
    }

    public void setConectados(Map<String, String> conectados) {
        this.conectados = conectados;
    }

}

class Items implements Serializable {

    private String myKey, myValue;

    public Items(String key, String value) {
        myKey = key;
        myValue = value;
    }

    public String getKey() {
        return myKey;
    }

    public String getValue() {
        return myValue;
    }

    public String toString() {
        if(myKey.equals("Todos")){
            return myKey;
        }
        else {
            return myValue + "-" + myKey;
        }
    }

}

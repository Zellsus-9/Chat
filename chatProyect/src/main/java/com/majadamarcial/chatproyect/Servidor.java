/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.majadamarcial.chatproyect;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 *
 * @author irene
 */
public class Servidor {

    public static void main(String[] args) {
        MarcoServidor miMarco = new MarcoServidor();
        miMarco.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}

class MarcoServidor extends JFrame implements Runnable, ActionListener {

    private JPanel lamina;
    private JLabel tituloBan, tituloMen;
    private JTextArea mensajes;
    private JScrollPane scrollMensajes;
    private Thread miHilo;
    private JComboBox listaBan;
    private JTextField ip;
    private JButton banearDes;
    private PaqueteEnvios paqueteRecibido, datos;
    private String nick, mensaje, ipRemota;
    private Items destino;
    private int puertoServer, puertoCliente;
    private Boolean conectando, desconectando;
    private Map<String, String> listaUsuarios, baneadosCon;
    private ArrayList baneados;
    private InetAddress direccion;
    private ServerSocket servidor;
    private Socket miSocket, enviaDestinatario;
    private ObjectInputStream paqueteDatos;
    private ObjectOutputStream paqueteReenvio;

    public MarcoServidor() {
        puertoServer = 9999;
        puertoCliente = 9090;

        setBounds(10, 10, 900, 410);
        lamina = new JPanel();
        tituloBan = new JLabel("############################################################## BANEADOS ##############################################################");
        lamina.add(tituloBan);
        listaBan = new JComboBox();
        listaBan.addItem("");
        lamina.add(listaBan);
        ip = new JTextField(30);
        lamina.add(ip);
        banearDes = new JButton("Banear/Desbanear");
        banearDes.addActionListener(this);
        lamina.add(banearDes);
        tituloMen = new JLabel("############################################################## MENSAJES ##############################################################");
        lamina.add(tituloMen);
        mensajes = new JTextArea(17, 80);
        mensajes.setEditable(false);
        scrollMensajes = new JScrollPane(mensajes);
        lamina.add(scrollMensajes);
        add(lamina);
        setVisible(true);

        miHilo = new Thread(this);
        miHilo.start();
    }

    @Override
    public void run() {
        try {
            servidor = new ServerSocket(puertoServer);
            listaUsuarios = new HashMap<String, String>();
            baneadosCon = new HashMap<String, String>();
            baneados = new ArrayList<String>();

            while (true) {
                miSocket = servidor.accept();
                paqueteDatos = new ObjectInputStream(miSocket.getInputStream());
                paqueteRecibido = (PaqueteEnvios) paqueteDatos.readObject();
                nick = paqueteRecibido.getNick();
                destino = paqueteRecibido.getDestino();
                mensaje = paqueteRecibido.getMensaje();
                conectando = paqueteRecibido.getConectando();
                desconectando = paqueteRecibido.getDesconectando();

                direccion = miSocket.getInetAddress();
                ipRemota = direccion.getHostAddress();

                if (baneados.contains(ipRemota)) {
                    if (conectando) {
                        baneadosCon.put(ipRemota, nick);
                        mensajes.append(nick + " ip baneada: " + ipRemota + " intento conectar\n");
                        paqueteRecibido.setMensaje("Tu IP ha sido baneada no puedes conectarte");
                        paqueteRecibido.setConectando(false);
                    } else if (desconectando) {
                        baneadosCon.remove(ipRemota);
                        mensajes.append(nick + " ip baneada: " + ipRemota + " se ha desconectado\n");
                    } else {
                        mensajes.append(nick + " ip baneada: " + ipRemota + ": " + mensaje + "\n");
                        paqueteRecibido.setMensaje("Tu IP ha sido baneada no puedes enviar mensajes");
                    }
                    enviaDestinatario = new Socket(ipRemota, puertoCliente);
                    paqueteReenvio = new ObjectOutputStream(enviaDestinatario.getOutputStream());
                    paqueteReenvio.writeObject(paqueteRecibido);
                    paqueteReenvio.close();
                } else if (conectando || desconectando || destino.getKey().equals("Todos")) {
                    if (conectando) {
                        listaUsuarios.put(ipRemota, nick);
                        mensajes.append(nick + "-" + ipRemota + " se ha conectado\n");
                        paqueteRecibido.setMensaje(nick + "-" + ipRemota + " se ha conectado");
                        paqueteRecibido.setConectando(true);
                    } else if (desconectando) {
                        listaUsuarios.remove(ipRemota);
                        mensajes.append(nick + "-" + ipRemota + " se ha desconectado\n");
                        paqueteRecibido.setMensaje(nick + "-" + ipRemota + " se ha desconectado");
                        paqueteRecibido.setDesconectando(true);
                    } else {
                        mensajes.append(nick + "-" + ipRemota + ": " + mensaje + "\n");
                        paqueteRecibido.setMensaje(nick + ": " + mensaje);
                    }
                    paqueteRecibido.setConectados(listaUsuarios);
                    for (String ips : listaUsuarios.keySet()) {
                        enviaDestinatario = new Socket(ips, puertoCliente);
                        paqueteReenvio = new ObjectOutputStream(enviaDestinatario.getOutputStream());
                        paqueteReenvio.writeObject(paqueteRecibido);
                        paqueteReenvio.close();
                    }
                } else {
                    mensajes.append(nick + "-" + ipRemota + " -> " + destino.toString() + ":" + mensaje + "\n");
                    paqueteRecibido.setMensaje(nick + " -> " + destino.getValue() + ": " + mensaje);
                    enviaDestinatario = new Socket(destino.getKey(), puertoCliente);
                    paqueteReenvio = new ObjectOutputStream(enviaDestinatario.getOutputStream());
                    paqueteReenvio.writeObject(paqueteRecibido);
                    paqueteReenvio.close();
                }

                miSocket.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(MarcoServidor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(MarcoServidor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!ip.getText().trim().equals("")) {
            try {
                baneados.add(ip.getText());
                listaBan.addItem(ip.getText());
                mensajes.append("Ip baneada: " + ip.getText() + "\n");

                if (listaUsuarios.containsKey(ip.getText())) {
                    baneadosCon.put(ip.getText(), listaUsuarios.get(ip.getText()));
                    listaUsuarios.remove(ip.getText());

                    miSocket = new Socket(ip.getText(), puertoCliente);
                    datos = new PaqueteEnvios();
                    datos.setDesconectando(true);
                    datos.setConectados(listaUsuarios);
                    datos.setMensaje("Ip baneada: " + ip.getText());
                    paqueteReenvio = new ObjectOutputStream(miSocket.getOutputStream());
                    paqueteReenvio.writeObject(datos);
                    paqueteReenvio.close();

                    for (String ips : listaUsuarios.keySet()) {
                        miSocket = new Socket(ips, puertoCliente);
                        datos = new PaqueteEnvios();
                        datos.setDesconectando(true);
                        datos.setConectados(listaUsuarios);
                        datos.setMensaje("Ip baneada: " + ip.getText());
                        paqueteReenvio = new ObjectOutputStream(miSocket.getOutputStream());
                        paqueteReenvio.writeObject(datos);
                        paqueteReenvio.close();
                    }
                }
                ip.setText("");
            } catch (IOException ex) {
                Logger.getLogger(MarcoServidor.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (!listaBan.getSelectedItem().toString().equals("")) {
            try {
                baneados.remove(listaBan.getSelectedItem().toString());
                mensajes.append("Ip desbaneada: " + listaBan.getSelectedItem().toString() + "\n");
                if (baneadosCon.containsKey(listaBan.getSelectedItem().toString())) {
                    listaUsuarios.put(listaBan.getSelectedItem().toString(), baneadosCon.get(listaBan.getSelectedItem().toString()));
                    baneadosCon.remove(listaBan.getSelectedItem().toString());

                    for (String ips : listaUsuarios.keySet()) {
                        miSocket = new Socket(ips, puertoCliente);
                        datos = new PaqueteEnvios();
                        datos.setConectando(true);
                        datos.setConectados(listaUsuarios);
                        datos.setMensaje("Ip desbaneada: " + listaBan.getSelectedItem().toString());
                        paqueteReenvio = new ObjectOutputStream(miSocket.getOutputStream());
                        paqueteReenvio.writeObject(datos);
                        paqueteReenvio.close();
                    }
                }
                listaBan.removeItem(listaBan.getSelectedItem().toString());
            } catch (IOException ex) {
                Logger.getLogger(MarcoServidor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        ip.setText("");
    }
}

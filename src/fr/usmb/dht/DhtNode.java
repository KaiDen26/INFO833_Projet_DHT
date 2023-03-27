package fr.usmb.dht;

import peersim.edsim.*;
import peersim.core.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import peersim.config.*;

public class DhtNode implements EDProtocol {
    
    // identifiant de la couche transport
    private int transportPid;

    // objet couche transport
    private HWTransport transport;

    // identifiant de la couche courante (la couche applicative)
    private int mypid;

    // le numero de noeud dans l'ordre de création
    private int id;
    
    // l'identifiant du noeud
    private int uid;

	// prefixe de la couche (nom de la variable de protocole du fichier de config)
    private String prefix;
    
    private DhtNode rightNeighbor;
    private DhtNode leftNeighbor;
    
    private HashMap<DhtNode, Map<Integer, Message>> dataInfos = new HashMap<>();

    public DhtNode(String prefix) {
    	
		this.prefix = prefix;
		// initialisation des identifiants a partir du fichier de configuration
		this.transportPid = Configuration.getPid(prefix + ".transport");
		this.mypid = Configuration.getPid(prefix + ".myself");
		this.transport = null;
		
    }
    


    // methode appelee lorsqu'un message est recu par le protocole HelloWorld du noeud
    public void processEvent( Node node, int pid, Object event ) {
    	
    	this.receive((Message)event);
    	
    }
    
    // methode necessaire pour la creation du reseau (qui se fait par clonage d'un prototype)
    public Object clone() {

		DhtNode dolly = new DhtNode(this.prefix);
	
		return dolly;
    }

    // liaison entre un objet de la couche applicative et un 
    // objet de la couche transport situes sur le meme noeud
    
    public void setTransportLayer(int nodeId) {
    	this.id = nodeId;
    	this.transport = (HWTransport) Network.get(this.id).getProtocol(this.transportPid);
    }

    // envoi d'un message (l'envoi se fait via la couche transport)
    public void send(Message msg, Node dest) {
    	msg.addSender(this);
    	this.transport.send(getMyNode(), dest, msg, this.mypid);
    }

    // affichage lors de la reception
    private void receive(Message msg) {
    	
    	DhtNode sender = msg.getLastSender();
    	
    	String prefixMsg = "[" + msg.getType().getDescription() + "] from " + msg.getLastSender().getUid() + " to " + this.uid + " -> ";
    	
    	switch (msg.type) {
		case JOIN: { 
			
			if(sender.equals(msg.getNodeToPlace())) {
				System.out.println("[" + msg.getType().getDescription() + "] from " + msg.getLastSender().getUid() + " -> I join the network");
			}
			
			if(this.leftNeighbor == this && this.rightNeighbor == this) {
				
				Message changeBothMsg = new Message(MessageType.PLACE_BOTH, "Change your neighbors !", new DhtNode[] {this, this});
				this.send(changeBothMsg, Network.get(msg.getNodeToPlace().getId()));
				
				this.rightNeighbor = msg.getNodeToPlace();
				this.leftNeighbor = msg.getNodeToPlace();
				System.out.println(prefixMsg + "Change Left Node to " + this.leftNeighbor.getUid() + " | Change Right Node to " + this.rightNeighbor.getUid());
				
				
				break;
			}
			
			if(msg.getNodeToPlace().getUid() > this.uid) {
				
				if(msg.getNodeToPlace().getUid() < this.rightNeighbor.getUid() || (this.uid > this.rightNeighbor.getUid())) {
					
					Message changeBothMsg = new Message(MessageType.PLACE_BOTH, "Change your neighbors !", new DhtNode[] {this, this.rightNeighbor});
					this.send(changeBothMsg, Network.get(msg.getNodeToPlace().getId()));
					
					Message changeLeftMsg = new Message(MessageType.PLACE_LEFT, "Change your left neighbor !", msg.getNodeToPlace());
					
					this.send(changeLeftMsg, Network.get(this.rightNeighbor.getId()));

					this.rightNeighbor = msg.getNodeToPlace();
					System.out.println(prefixMsg + "Change Right Node to " + this.rightNeighbor.getUid());
					
				} else {
					
					this.send(msg, Network.get(this.rightNeighbor.getId()));
					
					if(sender.equals(msg.getNodeToPlace())) {
						System.out.println(prefixMsg + "Try to place me");
					} else {
						System.out.println(prefixMsg + "Try to place " + msg.getNodeToPlace().getUid());
					}
				}
				
			} else {

				if(msg.getNodeToPlace().getUid() > this.leftNeighbor.getUid()  || (this.uid < this.leftNeighbor.getUid())) {
					
					
					Message changeBothMsg = new Message(MessageType.PLACE_BOTH, "Change your neighbors !", new DhtNode[] {this.leftNeighbor, this});
					this.send(changeBothMsg, Network.get(msg.getNodeToPlace().getId()));
					
					Message changeRightMsg = new Message(MessageType.PLACE_RIGHT, "Change your right neighbor !", msg.getNodeToPlace());
					
					this.send(changeRightMsg, Network.get(this.leftNeighbor.getId()));
					this.leftNeighbor = msg.getNodeToPlace();
					System.out.println(prefixMsg + "Change Left Node to " + this.leftNeighbor.getUid());
					
					
				} else {
					
					this.send(msg, Network.get(this.leftNeighbor.getId()));
					
					if(sender.equals(msg.getNodeToPlace())) {
						System.out.println(prefixMsg + "Try to place me");
					} else {
						System.out.println(prefixMsg + "Try to place " + msg.getNodeToPlace().getUid());
					}
					
				}
				
			}
			
			break;
		}
		case LEAVE: {
			
			Message changeLeftMsg = new Message(MessageType.PLACE_LEFT, "Change your left neighbor !", this.leftNeighbor);
			Message changeRightMsg = new Message(MessageType.PLACE_RIGHT, "Change your right neighbor !", this.rightNeighbor);
			
			this.send(changeLeftMsg, Network.get(this.rightNeighbor.getId()));
			this.send(changeRightMsg, Network.get(this.leftNeighbor.getId()));
			
			System.out.println("[" + msg.getType().getDescription() + "] from " + this.uid + " -> I leave the network");
			
			break;
		}
		case PLACE_BOTH: {
			
			this.leftNeighbor = msg.getNeighbors()[0];
			this.rightNeighbor = msg.getNeighbors()[1];
			
			System.out.println(prefixMsg + "Change Left Node to " + this.leftNeighbor.getUid() + " | Change Right Node to " + this.rightNeighbor.getUid());
			
			break;
		}
		case PLACE_LEFT: {
			
			this.leftNeighbor = msg.getNodeToPlace();
			
			System.out.println(prefixMsg + "Change Left Node to " + this.leftNeighbor.getUid());
			
			break;
		}
		case PLACE_RIGHT: {
			
			this.rightNeighbor = msg.getNodeToPlace();
			
			System.out.println(prefixMsg + "Change Right Node to " + this.rightNeighbor.getUid());
			
			
			break;
		}
		case ADD_DATA: {
			
			if(msg.getRemaining() > 0) {
				
				msg.decreaseRemaining();
				
				if(!msg.getSenders().contains(this.leftNeighbor) && !msg.getSenders().contains(this.rightNeighbor)) {
					this.send(msg, Network.get(this.rightNeighbor.getId()));
					this.send(msg, Network.get(this.leftNeighbor.getId()));
				}
				else if(msg.getSenders().contains(this.leftNeighbor)) {
					
					this.send(msg, Network.get(this.rightNeighbor.getId()));
					
				} else {
					
					this.send(msg, Network.get(this.leftNeighbor.getId()));
					
				}
				
				
			}
			
			if(this.dataInfos.keySet().contains(sender)) {
	    		Map<Integer, Message> currentInfos = this.dataInfos.get(sender);
	    		currentInfos.put(Controller.generateNewDataId(), msg.data2Save());
	    		this.dataInfos.put(sender, currentInfos);
	    		
	    	} else {
	    		Map<Integer, Message> newInfos = new HashMap<>();
	    		newInfos.put(Controller.generateNewDataId(), msg.data2Save());
	    		this.dataInfos.put(sender, newInfos);
	    	}
			
			break;
			
		}
		case SHOW_TREE: {
			
			System.out.println(getTree("Tree :\n", this, this));
			break;
			
		}
		
		default:
			throw new IllegalArgumentException("Unexpected value");
		}
    	
    	if(!msg.isType(MessageType.ADD_DATA)) {
    		Message addData = new Message(MessageType.ADD_DATA, "Update your data !", msg);
    		addData.setRemaining(3);
        	this.send(addData, getMyNode());
    	}
    	
    }
    
    public void showInfos() {
    	
    	System.err.println("\n\n >>>>> Data Informations for " + this + " <<<<<");
    	
    	for(DhtNode currentNode : this.dataInfos.keySet()) {
    		
    		System.out.println("-------- " + currentNode + " --------");
    		
    		Collection<Message> data = this.dataInfos.get(currentNode).values();
    		
    		for(Message messages : data) {
    			
    			System.out.println("- " + messages + " (" + messages.getContent() + ")");
    			
    		}
    		
    		
    	}
    	
    }
    
    private String getTree(String message, DhtNode startingNode, DhtNode currentNode) {
    	
		if(!currentNode.getRightNeighor().equals(startingNode)) {
			
			message += " " + currentNode.getUid() + " -> ";
			
			return getTree(message, startingNode, currentNode.getRightNeighor());
			
		} else {
			
			message += " " + currentNode.getUid();
			
		}
		
		return message;
    	
    }

    // retourne le noeud courant
    
    private Node getMyNode() {
    	return Network.get(this.id);
    }

    public String toString() {
    	return "Node "+ this.id;
    }
    
    /**
     * Fonction pour récupérer tous les voisins
     * 
     */
    public List<DhtNode> getAllNeighors(){
    	
    	List<DhtNode> allNeighbors = new ArrayList<>();
    	allNeighbors.add(this.rightNeighbor);
    	allNeighbors.add(this.leftNeighbor);
    	
    	return allNeighbors;
    	
    }
    
    public Integer getId() {
  		return this.id;
  	}
    
    public Integer getUid() {
  		return this.uid;
  	}
    
    public void setNodeUid(int nodeUid) {
  		this.uid = nodeUid;
  	}
    
    /**
     * Fonction pour récupérer le voisin de droite
     * 
     */
    public DhtNode getRightNeighor(){
    	
    	return this.rightNeighbor;
    	
    }
    
    /**
     * Fonction pour récupérer le voisins de gauche
     * 
     */
    public DhtNode getLeftNeighor(){
    	
    	return this.leftNeighbor;
    	
    }
    
    /**
     * Fonction pour ajouter les voisins
     * 
     */
    public void setNeighbors(DhtNode leftNode, DhtNode rightNode) {
    	
    	this.leftNeighbor = leftNode;
    	this.rightNeighbor = rightNode;
    	
    }
    
    /**
     * Fonction pour ajouter un voisin à droite
     * 
     */
    public void setRightNeighbor(DhtNode node) {
    	
    	this.rightNeighbor = node;
    	
    }
    
    /**
     * Fonction pour ajouter un voisin à gauche
     * 
     */
    public void setLeftNeighbor(DhtNode node) {
    	
    	this.leftNeighbor = node;
    	
    }
    
    /**
     * Fonction pour savoir si ce noeud a pour voisin un autre noeud
     * 
     */
    public boolean containsNeighbor(DhtNode node) {
    	
    	return this.leftNeighbor == node || this.rightNeighbor == node;
    	
    }

}